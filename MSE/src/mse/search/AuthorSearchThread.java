/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse.search;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

// mse
import mse.common.*;
import mse.data.Author;
import mse.data.BibleBook;
import mse.data.HymnBook;
import mse.helpers.FileHelper;
import mse.helpers.HtmlHelper;

/**
 * @author michael
 */
public class AuthorSearchThread extends SingleSearchThread {

    private Config cfg;
    private ArrayList<LogRow> searchLog;

    AuthorSearchCache asc;

    private ArrayList<String> authorResults;

    private AtomicInteger progress;

    public AuthorSearchThread(Config cfg, AuthorSearchCache asc, AtomicInteger progress) {
        this.cfg = cfg;
        this.searchLog = new ArrayList<>();
        this.authorResults = new ArrayList<>();
        this.progress = progress;

        this.asc = asc;
    }

    @Override
    public void run() {

        searchAuthor(authorResults, asc);

        authorResults.add(HtmlHelper.getSingleAuthorResults(asc.getAuthorName(), asc.numAuthorResults));

    }

    private void searchAuthor(ArrayList<String> resultText, AuthorSearchCache asc) {

        log(LogLevel.DEBUG, "\tSearching: " + asc.author.getName() + " for \"" + asc.getSearchString() + "\"");

        // get the search words
        asc.setSearchWords();

        // add the author header
        HtmlHelper.getAuthorResultsHeader(asc.author, asc.printableSearchWords()).forEach(resultText::add);
        log(LogLevel.TRACE, "\tSearch strings: " + asc.printableSearchWords());

        /* Search all the words to make sure that all the search tokens are in the author's
         * index. log any words that are too frequent, find the least frequent token and
         * record the number of infrequent words
         */
        if (asc.getWildSearch()) {
            asc.setSearchTokens(asc.getSearchWords());
        } else {
            asc.setSearchTokens(tokenizeArray(asc.getSearchWords(), asc.author.getCode(), 0, 0));
        }
        log(LogLevel.TRACE, "\tSearch tokens: " + asc.printableSearchTokens());

        int errorNum = asc.setLeastFrequentToken();

        if ((asc.getLeastFrequentToken() != null) && (errorNum % 4 < 2)) {
            // at least one searchable token and all tokens in author index

            // add all the references for the least frequent token to the referencesToSearch array
            asc.setReferencesToSearch();

            // if there is more than one infrequent word
            // refine the number of references (combine if wild,
            // if not wild only use references where each word is found within 1 page
            asc.refineReferences();

            // TODO what is option.fullScan

            // if there are still any references to search

            // should be at least two references (volume and page to start)
            if (asc.referencesToSearch.length > 1) {

                // process each page that contains a match
                asc.refIndex = 0;

                // first two references should be volume and page
                asc.getNextPage();

                boolean volumeSuccess = true;

                // for each reference
                while (asc.volNum != 0 && volumeSuccess) {

                    if (asc.author.equals(Author.HYMNS)) {
                        resultText.add(String.format("\t\t<p class=\"%s\"><a href=\"%s\">%s</a></p>",
                                "results-hymnbook-name",
                                "..\\..\\" + asc.author.getTargetPath(getVolumeName()),
                                HymnBook.values()[asc.volNum - 1].getName()));
                    }

                    volumeSuccess = searchSingleVolume(resultText, asc);

                } // end one frequent token and all tokens found

            } // end had references to search
            else {
                log(LogLevel.LOW, "No overlap in references for " + asc.author.getCode());
            }

        } else {
            resultText.add("\t<p>");
            if (errorNum % 4 > 1) {
                resultText.add("\t\tNot all words were found in index.<br>");
                searchLog.add(new LogRow(LogLevel.LOW, "\tTokens in " + asc.author.getCode() + " not all found: " + asc.printableSearchTokens()));
            }
            if (errorNum >= 4) {
                resultText.add("\t\tSome words appeared too frequently: " + asc.getTooFrequentTokens());
                searchLog.add(new LogRow(LogLevel.LOW, "\tToo frequent tokens in " + asc.author.getCode() + ": " + asc.getTooFrequentTokens()));
            }
            resultText.add("\t</p>");
        }

        resultText.add(HtmlHelper.closeAuthorResultsBlock());
    }

    private boolean searchSingleVolume(ArrayList<String> resultText, AuthorSearchCache asc) {
        // for each volume

        searchLog.add(new LogRow(LogLevel.TRACE, "\tVol: " + asc.volNum));

        int cPageNum = 0;
        final int cVolNum = asc.volNum;

        // get file name
        String filename = FileHelper.getHtmlFileName(cfg, asc.author, asc.volNum);

        HtmlReader htmlReader = new HtmlReader(filename, searchLog);
        // start with the first line
        asc.line = htmlReader.readContentLine();

        progress.addAndGet(1000 / asc.author.getNumVols());

        while (asc.volNum == cVolNum) {
            // while still in the same volume
            // loop through references

            asc.prevLine = "";

            // skip to next page and get the last line of the previous page
            cPageNum = htmlReader.findNextPage(asc);

            // if the page number is 0 log the error and break out
            if (cPageNum == 0) {
                log(LogLevel.HIGH, "Could not find reference " + asc.getShortReadableReference());
                htmlReader.close();
                return false;
            }

            asc.currentSectionHeader = htmlReader.getFirstSectionHeader(asc);
            if (asc.currentSectionHeader == null) {
                searchLog.add(new LogRow(LogLevel.HIGH, "NULL line " + asc.getShortReadableReference()));
            } else {
                searchSinglePage(resultText, asc, htmlReader);
            }
            asc.line = asc.currentSectionHeader;

            // clear the previousLine
            asc.prevLine = "";

            // get the next reference
            asc.getNextPage();

        } // finished references in volume

        htmlReader.close();
        return true;

    }

    private void searchSinglePage(ArrayList<String> resultText, AuthorSearchCache asc, HtmlReader htmlReader) {

        boolean foundToken = false;

        asc.setVerseNum(0);

        // while still on the same page (class != page-number)
        while (isNextSectionSearchable(asc.currentSectionHeader)) {

            asc.line = htmlReader.getNextSection(asc);

            ArrayList<String> stringsToSearch = new ArrayList<>();

            // get the searchLine based on the search scope
            if ((asc.getSearchScope() == SearchScope.SENTENCE) || asc.getSearchScope() == SearchScope.CLAUSE) {
                stringsToSearch = convertLineIntoSentences(asc.line, getTrailingIncompleteSentence(asc.prevLine));
            } else {
                stringsToSearch.add(asc.line);
            }

            asc.incrementVerseNum();

            // search the scope
            foundToken = searchScope(resultText, stringsToSearch, asc, foundToken);

            asc.setFoundDarby(foundToken);

            if (asc.author.equals(Author.BIBLE)) {
                resultText = asc.finishSearchingSingleBibleScope(removeHtml(asc.line), resultText, foundToken);
                if (asc.getSearchScope() == SearchScope.CLAUSE) {
                    foundToken = false;
                }
            }

            // set the current line as the previous line if it is searchable
            if (isNextSectionSearchable(asc.currentSectionHeader)) {
                asc.prevLine = asc.line;
            } else {
                asc.prevLine = "";
            }

            asc.currentSectionHeader = htmlReader.getNextSectionHeader(asc);
        }

        if (!foundToken) {
            if (asc.getInfrequentTokens().size() > 1) {
                searchLog.add(new LogRow(LogLevel.DEBUG, "Did not find token " + asc.getShortReadableReference()));
            } else {
                searchLog.add(new LogRow(LogLevel.LOW, "Did not find token " + asc.getShortReadableReference()));
            }
        }
    }

    private boolean isNextSectionSearchable(String sectionHeader) {

        if (asc.author.isMinistry()) {
            return sectionHeader.contains("class=\"heading\"") || sectionHeader.contains("class=\"paragraph\"") || sectionHeader.contains("class=\"footnote\"");
        } else if (asc.author.equals(Author.BIBLE)) {
            return sectionHeader.contains("<tr");
        } else if (asc.author.equals(Author.HYMNS)) {
            return sectionHeader.contains("class=\"verse-number\"");
        } else {
            return false;
        }
    }

    private boolean searchScope(ArrayList<String> resultText, ArrayList<String> stringsToSearch, AuthorSearchCache asc, boolean foundToken) {

        for (String scope : stringsToSearch) {

            boolean validScope = false;

            switch (asc.getSearchScope()) {
                case SENTENCE:
                    validScope = checkSentenceSearch(scope, asc);
                    break;
                case CLAUSE:
                    validScope = clauseSearch(tokenizeLine(scope, asc), asc.getSearchTokens());
            }


            // if the current scope contains all search terms mark them and print it out (or one if it is a wildcard search)
            if (validScope) {

                foundToken = true;

                String markedLine = markLine(new StringBuilder(scope), asc.getSearchWords(), "mse-mark");

//                // close any opened blockquote tags
//                if (markedLine.contains("<blockquote>")) markedLine += "</blockquote>";

                addResultText(resultText, markedLine);

                asc.incrementResults();
            }
        }

        return foundToken;
    }

    private void addResultText(ArrayList<String> resultText, String markedLine) {

        if (asc.author.isMinistry()) {

            resultText.addAll(HtmlHelper.getMinistryResultBlock(
                    asc.author.getTargetPath(getVolumeName() + "#" + asc.pageNum),
                    getReadableReference(),
                    markedLine));

        } else if (asc.author.equals(Author.BIBLE)) {

            HtmlHelper.writeBibleResultBlock(resultText, asc, getVolumeName(), getReadableReference(), markedLine);

        } else if (asc.author.equals(Author.HYMNS)) {

            resultText.addAll(HtmlHelper.getHymnsResultBlock(asc.author.getTargetPath(getVolumeName() + "#" + asc.pageNum), getReadableReference(), markedLine));

        }

    }

    private String getReadableReference() {
        if (asc.author.isMinistry()) {
            return asc.author.getCode() + " volume " + asc.volNum + " page " + asc.pageNum;
        } else if (asc.author.equals(Author.BIBLE)) {
            return BibleBook.values()[asc.volNum - 1].getName() + " chapter " + asc.pageNum + ":" + asc.getVerseNum();
        } else if (asc.author.equals(Author.HYMNS)) {
            return Integer.toString(asc.pageNum);
        }

        return "";
    }

    private String getVolumeName() {
        if (asc.author.isMinistry()) {
            return asc.author.getCode() + asc.volNum + ".htm";
        } else if (asc.author.equals(Author.BIBLE)) {
            return BibleBook.values()[asc.volNum - 1].getName() + ".htm";
        } else if (asc.author.equals(Author.HYMNS)) {
            return HymnBook.values()[asc.volNum - 1].getOutputFilename();
        } else {
            return "";
        }
    }


    String debugLine;

    // check sentence search
    private boolean checkSentenceSearch(String scope, AuthorSearchCache asc) {
        return wordSearch(tokenizeLine(scope, asc), asc.getSearchTokens(), asc.getWildSearch());
    }

    boolean foundCurrentSearchToken;

    private boolean wordSearch(String[] currentLineTokens, String[] searchTokens, boolean wildSearch) {

        for (String nextSearchToken : searchTokens) {
            foundCurrentSearchToken = false;
            for (String nextLineToken : currentLineTokens) {
                if (nextSearchToken.equals(nextLineToken)) {
                    foundCurrentSearchToken = true;
                    if (wildSearch) return true;
                }
            }
            if (!foundCurrentSearchToken && !asc.getWildSearch()) return false;
        }

        // if it reaches this point as a wild search then no tokens were found
        if (wildSearch) return false;
        return true;
    }

    private boolean clauseSearch(String[] currentLineTokens, String[] searchTokens) {

        boolean toggle = false;

        // read through the clause finding each word in order
        // return false if reached the end without finding every word

        // true if the next word should be next word in the search tokens
        boolean currentWordIsSearchToken;

        // position of the next token to find in the search tokens array
        int j = 0;

        for (int i = 0; i < currentLineTokens.length; i++) {

            if (currentLineTokens[i].equals("")) continue;

            currentWordIsSearchToken = false;
            if (currentLineTokens[i].equalsIgnoreCase(searchTokens[j])) {
                j++;
                currentWordIsSearchToken = true;
            }

            if (j > 0) {

                // if all words found in order return true
                if (j == searchTokens.length) return true;

                // if current word wasn't a search token reset j
                if (!currentWordIsSearchToken) j = 0;
            }

        }

        return false;

    }

    int startOfLastSentencePos;

    private String getTrailingIncompleteSentence(String line) {

        if (asc.author.equals(Author.BIBLE)) return line;

        if (line.isEmpty()) return "";

        startOfLastSentencePos = line.lastIndexOf("<a name=");

        // no sentence break in line
        if (startOfLastSentencePos < 0) return line;

        startOfLastSentencePos = line.indexOf('>', startOfLastSentencePos) + 1;
        startOfLastSentencePos = line.indexOf('>', startOfLastSentencePos) + 1;

        if (startOfLastSentencePos < line.length()) return line.substring(startOfLastSentencePos);
        return "";
    }

    ArrayList<String> sentences = new ArrayList<>();
    int startOfSentencePos;
    int endOfSentencePos;

    private ArrayList<String> convertLineIntoSentences(String line, String trailingIncompleteSentence) {

        sentences.clear();
        startOfSentencePos = 0;
        endOfSentencePos = 0;

        if (!line.contains("<a name=")) {
            // if there are no fullstops return the whole line
            sentences.add(line);
            return sentences;
        }

        while (endOfSentencePos >= 0 && endOfSentencePos < line.length()) {

            endOfSentencePos = line.indexOf("<a name=", startOfSentencePos) - 1;

            // if there are no fullstops return the whole line
            if (endOfSentencePos < 0) {
                sentences.add(line.substring(startOfSentencePos, line.length()));
                return sentences;
            }

            sentences.add(line.substring(startOfSentencePos, endOfSentencePos));

            // skip the a tags
            startOfSentencePos = line.indexOf('>', endOfSentencePos) + 1;
            startOfSentencePos = line.indexOf('>', startOfSentencePos) + 1;
            endOfSentencePos = startOfSentencePos;

        }

        if (sentences.size() > 0) sentences.set(0, trailingIncompleteSentence + " " + sentences.get(0));

        return sentences;
    }

    StringBuilder lineBuilder = new StringBuilder();
    int startHtml;
    int endHtml;

    private String[] tokenizeLine(String line, AuthorSearchCache asc) {

        // if the line contains html - remove the html tag
        while (line.contains("<")) {

            // set the lineBuilder to the string passed in
            lineBuilder.setLength(0);
            lineBuilder.append(line);
            startHtml = 0;
            while ((startHtml < lineBuilder.length()) && (lineBuilder.charAt(startHtml) != '<')) {
                startHtml++;
            }
            endHtml = startHtml + 1;
            while ((endHtml < lineBuilder.length()) && (lineBuilder.charAt(endHtml) != '>')) {
                endHtml++;
            }
            if ((startHtml < lineBuilder.length()) && (endHtml <= lineBuilder.length()))
                lineBuilder.replace(startHtml, endHtml + 1, "");

            line = lineBuilder.toString();
        }

        // split the line into tokens (words) by non-word characters
        return tokenizeArray(line.split("[\\W]"), asc.getAuthorCode(), asc.volNum, asc.pageNum);
    }

    String[] newTokensArray;
    ArrayList<String> newTokens = new ArrayList<>();

    private String[] tokenizeArray(String[] tokens, String authorCode, int volNum, int pageNum) {

        newTokens.clear();

        // make each token into a word that can be searched
        for (String token : tokens) {
            token = token.toUpperCase();
            if (!isAlpha(token)) {
                token = processString(token);
            }
            if (!isAlpha(token)) {
                searchLog.add(new LogRow(LogLevel.HIGH, "Error processing token " + authorCode + " " + volNum + ":" + pageNum + ": " + token));
                token = "";
            }
            newTokens.add(token);
        } // end for each token

        newTokensArray = new String[newTokens.size()];
        newTokensArray = newTokens.toArray(newTokensArray);

        return newTokensArray;
    }

    private boolean isAlpha(String token) {
        char[] chars = token.toCharArray();

        for (char c : chars) {
            if (!Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }

    private String processString(String token) {
        for (char c : token.toCharArray()) {
            if (!Character.isLetter(c)) {
                token = token.replace(Character.toString(c), "");
            }
        }

        return token;
    }

    private String getBasicWords(String strIn, boolean dropPunctuation, boolean dropTableTags) {
        String inString = strIn;
        String outString = "";
        char currentChar;
        int currentPosition = 0;
        int numSpaces = 0;
        int numCols = 0;
        int lengthOfInString = inString.length();

        //remove html tags & page numbers
        while (currentPosition < lengthOfInString) {
            currentChar = inString.charAt(currentPosition);
            if ((currentChar == 13) || (currentChar == 10)) {
                //ignore carriage returns & line feeds

            } else if (currentChar == '<') {

                // get the position of an html starting tag
                int keepCurrPos = currentPosition;

                // find the positon of an html end tag
                while ((currentPosition < inString.length()) && (inString.charAt(currentPosition) != '>')) {
                    currentPosition++;
                }

                // TODO -I find out why you do this
                // if you haven't reached the end of the string to remove tags from and you don't have to drop table tags
                if ((currentPosition < inString.length()) && (!dropTableTags)) {

                    // get the contents of the tag
                    String htmlTag = inString.substring(keepCurrPos, currentPosition);

                    if ((htmlTag.equals("</td")) && (numCols < 2)) {
                        outString += "</td><td>";
                        numCols++;
                    } else if (htmlTag.equals("</tr")) {
                        outString += ("</td></tr><tr><td>");
                        numCols = 0;
                    }
                }

                // TODO -I find out what this does
                if ((inString.length() - currentPosition) > 10) {//possibly new page
                    String strNextBit = inString.toString().substring(currentPosition + 1, currentPosition + 7);
                    if (strNextBit.equals("[Page ")) {//throw away word 'page'
                        while ((currentPosition < inString.length()) && (inString.charAt(currentPosition) != ']')) {
                            currentPosition++;
                        }
                    }
                }

                // TODO -I find out why
                // if there are no spaces, add one on the end
                if (numSpaces == 0) {
                    outString += " ";
                    numSpaces++;
                }

                // TODO -I could this be else if
            } else {
                if (dropPunctuation) {

                    // if punctuation is to be dropped
                    if (Character.isLetter(currentChar) || Character.isDigit(currentChar)) {

                        // if the character is a letter or a digit
                        // add the current character the the output
                        outString += currentChar;
                        numSpaces = 0;

                    } else if (currentChar == '-' || currentChar == '\'') {
                        if ((Character.isLetter(outString.charAt(outString.length() - 1))) && (numSpaces == 0)) {
                            // if the last character in the output so far is a letter and the number of spaces is zero
                            outString += currentChar;
                        } else if (numSpaces == 0) {
                            outString += " ";
                            numSpaces++;
                        }
                    } else if (numSpaces == 0) {
                        outString += " ";
                        numSpaces++;
                    }
                } else {
                    // if punctation is kept just add the current character to the output
                    outString += currentChar;
                    numSpaces = 0;
                }
            }
            currentPosition++;
        }
        return outString;
    }

    int charPos = 0;
    int startOfWord = 0;
    int endOfWord = 0;
    String currentCapitalisation;

    private String markLine(StringBuilder line, String[] words, String emphasis) {
        // highlight all the search words in the line with an html <mark/> tag

        // remove any html already in the line
        charPos = 0;

        if (!asc.author.equals(Author.HYMNS)) line = removeHtml(line);

        for (String word : words) {

            charPos = 0;

            // while there are still more words matching the current word in the line
            // and the char position hasn't exceeded the line
            while (charPos < line.length() && ((startOfWord = line.toString().toLowerCase().indexOf(word.toLowerCase(), charPos)) != -1)) {

                endOfWord = startOfWord + word.length();
                charPos = endOfWord;

                // if the word has a letter before it or after it then skip it
                if (startOfWord >= 0 && Character.isLetter(line.charAt(startOfWord - 1))) continue;
                if (endOfWord < line.length() && Character.isLetter(line.charAt(endOfWord))) continue;

                // otherwise mark the word
                currentCapitalisation = line.substring(startOfWord, endOfWord);
                String openDiv = "<span class=\"" + emphasis + "\">";
                String closeDiv = "</span>";
                line.replace(startOfWord, endOfWord, openDiv + currentCapitalisation + closeDiv);

                // set the char position to after the word
                charPos = endOfWord + openDiv.length() + closeDiv.length();
            }
        }

        return line.toString();
    }

    private String removeHtml(String line) {
        return removeHtml(new StringBuilder(line)).toString();
    }

    private StringBuilder removeHtml(StringBuilder line) {
        int charPos = -1;

        while (++charPos < line.length()) {
            if (line.charAt(charPos) == '<') {
                int tempCharIndex = charPos + 1;
                while (tempCharIndex < line.length() - 1 && line.charAt(tempCharIndex) != '>') tempCharIndex++;
                tempCharIndex++;
                line.replace(charPos, tempCharIndex, "");
                charPos = 0;
            }
        }

        return line;
    }

    private void log(LogLevel logLevel, String message) {
        searchLog.add(new LogRow(logLevel, message));
    }

    @Override
    public ArrayList<LogRow> getLog() {
        return searchLog;
    }

    @Override
    public ArrayList<String> getResults() {
        return authorResults;
    }

    @Override
    int getNumberOfResults() {
        return asc.numAuthorResults;
    }
}
