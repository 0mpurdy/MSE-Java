/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse.search;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

// mse
import mse.common.*;
import mse.data.Author;
import mse.data.Reference;
import mse.data.Result;
import mse.helpers.FileHelper;
import mse.helpers.HtmlHelper;
import mse.helpers.HtmlReader;

/**
 * @author michael
 */
public class AuthorSearchThread extends SingleSearchThread {

    private Config cfg;
    private ArrayList<LogRow> searchLog;
    private AuthorSearchCache asc;
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

    // region search

    private void searchAuthor(ArrayList<String> resultText, AuthorSearchCache asc) {

        log(LogLevel.DEBUG, "\tSearching: " + asc.author.getName() + " for \"" + asc.getSearchString() + "\"");

        // get the search words
        asc.setSearchWords();

        // add the author header
        log(LogLevel.TRACE, "\tSearch strings: " + asc.printableSearchWords());

        /* Search all the words to make sure that all the search tokens are in the author's
         * index. log any words that are too frequent, find the least frequent token and
         * record the number of infrequent words
         */
        asc.setSearchTokens(searchLog);
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
                while (asc.reference.volNum != 0 && volumeSuccess) {

                    asc.notFoundCurrentHymnBook = true;
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

        searchLog.add(new LogRow(LogLevel.TRACE, "\tVol: " + asc.reference.volNum));

        int cPageNum = 0;
        final int cVolNum = asc.reference.volNum;

        // get file name
        String filename = FileHelper.getHtmlFileName(cfg, asc.author, asc.reference.volNum);

        HtmlReader htmlReader = new HtmlReader(filename, searchLog);
        // start with the first line
        asc.line = htmlReader.readContentLine();

        progress.addAndGet(1000 / asc.author.getNumVols());

        while (asc.reference.volNum == cVolNum) {
            // while still in the same volume
            // loop through references

            asc.prevLine = "";

            // skip to next page and get the last line of the previous page
            cPageNum = htmlReader.findNextAuthorPage(asc);

            // if the page number is 0 log the error and break out
            if (cPageNum == 0) {
                log(LogLevel.HIGH, "Could not find reference " + asc.reference.getShortReadableReference());
                htmlReader.close();
                return false;
            }

            asc.currentSectionHeader = htmlReader.getFirstAuthorSectionHeader(asc);
            if (asc.currentSectionHeader == null) {
                searchLog.add(new LogRow(LogLevel.HIGH, "NULL line " + asc.reference.getShortReadableReference()));
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

        asc.reference.verseNum = 0;

        // while still on the same page (class != page-number)
        while (isNextSectionSearchable(asc.currentSectionHeader)) {

            asc.line = htmlReader.getNextAuthorSection(asc);

            ArrayList<String> stringsToSearch = new ArrayList<>();

            // get the searchLine based on the search scope
            if ((asc.getSearchType() == SearchType.SENTENCE) || asc.getSearchType() == SearchType.MATCH || asc.getSearchType() == SearchType.PHRASE) {
                stringsToSearch = convertLineIntoSentences(asc.line, getTrailingIncompleteSentence(asc.prevLine));
            } else {
                stringsToSearch.add(asc.line);
            }

            asc.incrementVerseNum();

            // search the scope
            foundToken = searchScope(resultText, stringsToSearch, asc, foundToken);

            asc.setFoundDarby(foundToken);

            if (asc.author.equals(Author.BIBLE)) {
                resultText = HtmlHelper.finishSearchingSingleBibleScope(HtmlHelper.removeHtml(asc.line), resultText, asc, foundToken);
                if (asc.getSearchType() == SearchType.MATCH) {
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
            // if exact match only debug level as many references will not have exact matches
            if (asc.getSearchType() == SearchType.MATCH) {
                searchLog.add(new LogRow(LogLevel.DEBUG, "Did not find token " + asc.reference.getShortReadableReference()));
            } else {
                searchLog.add(new LogRow(LogLevel.LOW, "Did not find token " + asc.reference.getShortReadableReference()));
            }
        }
    }

    private boolean searchScope(ArrayList<String> resultText, ArrayList<String> stringsToSearch, AuthorSearchCache asc, boolean foundToken) {

        for (String scope : stringsToSearch) {

            boolean validScope = false;

            if (asc.getWildSearch()) {
                validScope = wordSearch(HtmlHelper.tokenizeLine(scope, asc, searchLog), asc.getSearchTokens(), asc.getWildSearch());
            } else {

                String[] tokenizedLine = HtmlHelper.tokenizeLine(scope, asc, searchLog);
                switch (asc.getSearchType()) {
                    case MATCH:
                        validScope = clauseSearch(tokenizedLine, asc.getSearchTokens());
                        break;
                    case PHRASE:
                        validScope = scopeWordsInOrder(tokenizedLine, asc.getSearchTokens());
                        break;
                    case SENTENCE:
                        validScope = wordSearch(tokenizedLine, asc.getSearchTokens(), asc.getWildSearch());
                        break;
                    case PARAGRAPH:
                        validScope = wordSearch(tokenizedLine, asc.getSearchTokens(), asc.getWildSearch());
                }
            }


            // if the current scope contains all search terms mark them and print it out (or one if it is a wildcard search)
            if (validScope) {

                foundToken = true;

                String markedLine = HtmlHelper.markLine(asc.author, new StringBuilder(scope), asc.getSearchWords(), "mse-mark");

                if (asc.author.equals(Author.HYMNS) && asc.notFoundCurrentHymnBook) {
                    asc.notFoundCurrentHymnBook = false;
                    HtmlHelper.writeHymnbookName(resultText, asc);
                }

                addResultText(resultText, markedLine);

                asc.incrementResults();
            }
        }

        return foundToken;
    }

    private boolean scopeWordsInOrder(String[] lineTokens, String[] searchTokens) {
        int indexNextLineToken = 0;
        int indexNextSearchToken = 0;

        while (indexNextLineToken < lineTokens.length) {
            if (lineTokens[indexNextLineToken].toUpperCase().equals(searchTokens[indexNextSearchToken])) {
                indexNextSearchToken++;

                // if no more tokens to find
                if (indexNextSearchToken == searchTokens.length) return true;
            }
            indexNextLineToken++;
        }

        // if gone through all the line and not found all the tokens in the order
        return false;
    }

    // endregion

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

    private void addResultText(ArrayList<String> resultText, String markedLine) {

        if (asc.author.equals(Author.BIBLE)) {
            HtmlHelper.writeBibleResultBlock(resultText, asc, markedLine);
        } else {
            Result result = new Result(asc.author, new Reference(asc.author, asc.reference), markedLine, asc.searchWords);
            resultText.add(result.getHymnsResultBlock());
        }
    }

    // region checkValidScope

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
        return !wildSearch;
    }

    private boolean clauseSearch(String[] currentLineTokens, String[] searchTokens) {

        boolean toggle = false;

        // read through the clause finding each word in order
        // return false if reached the end without finding every word

        // true if the next word should be next word in the search tokens
        boolean currentWordIsSearchToken;

        // position of the next token to find in the search tokens array
        int j = 0;

        //
        for (int i = 0; i < currentLineTokens.length; i++) {

            try {
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

            } catch (NullPointerException npe) {
                System.out.println("debug");
            }
        }

        return false;

    }

    // endregion

    private int startOfLastSentencePos;

    // region processSection

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

    // endregion

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

    private void log(LogLevel logLevel, String message) {
        searchLog.add(new LogRow(logLevel, message));
    }

    // region singleSearchThreadMethods

    @Override
    public ArrayList<LogRow> getLog() {
        return searchLog;
    }

    @Override
    public ArrayList<String> getResults() {
        return authorResults;
    }

    @Override
    public int getNumberOfResults() {
        return asc.numAuthorResults;
    }

    public AuthorSearchCache getAsc() {
        return asc;
    }

    // endregion
}
