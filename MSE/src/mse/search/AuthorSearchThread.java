/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse.search;

// gui

// java
import java.io.*;
        import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

// mse
import mse.common.*;
import mse.data.Author;
import mse.data.AuthorIndex;
import mse.data.BibleBook;
import mse.data.Search;

/**
 * @author michael
 */
public class AuthorSearchThread extends SingleSearchThread {

    private Config cfg;
    private ArrayList<LogRow> searchLog;
    private AuthorIndex authorIndex;
    private Search search;

    private ArrayList<String> authorResults;

    private AtomicInteger progress;

    public AuthorSearchThread(Config cfg, AuthorIndex authorIndex, Search search, AtomicInteger progress) {
        this.cfg = cfg;
        this.authorIndex = authorIndex;
        this.search = search;
        this.searchLog = new ArrayList<>();
        this.authorResults = new ArrayList<>();
        this.progress = progress;
    }

    @Override
    public void run() {

//        logger.log(LogLevel.DEBUG, "\tStarted Search: \"" + search.getSearchString() + "\" in " + authorsToSearch.toString());
//        logger.log(LogLevel.DEBUG, "\tOpened file: " + cfg.getResDir() + cfg.getResultsFileName());
//        resultText.add(writeHtmlHeader());
//        // for each author to be searched
//        for (Author nextAuthor : authorsToSearch) {
//
//            if (nextAuthor == Author.HYMNS) {
//                logger.log(LogLevel.LOW, "Hymns search doesn't work yet");
//                continue;
//            }
//            if (nextAuthor == Author.BIBLE) {
//                logger.log(LogLevel.LOW, "Bible search doesn't work yet");
//                continue;
//            }

        AuthorSearchCache asc = new AuthorSearchCache(cfg, authorIndex, search);

        searchAuthor(authorResults, asc, search);

        authorResults.add("Number of results for " + authorIndex.getAuthorName() + ": " + asc.numAuthorResults);

    }

    private void searchAuthor(ArrayList<String> resultText, AuthorSearchCache asc, Search search) {

        searchLog.add(new LogRow(LogLevel.DEBUG, "\tSearching: " + asc.author.getName() + " for \"" + search.getSearchString() + "\""));

        // get the search words
        asc.setSearchWords(authorIndex);

        // print the title of the author search results and search words
        resultText.add("\n\t<hr>\n\t<h1>Results of search through " + asc.author.getName() + "</h1>");
        resultText.add("\n\t<p>\n\t\tSearched: " + asc.printableSearchWords() + "\n\t</p>");
        searchLog.add(new LogRow(LogLevel.TRACE, "\tSearch strings: " + asc.printableSearchWords()));

        /* Search all the words to make sure that all the search tokens are in the author's
         * index. log any words that are too frequent, find the least frequent token and
         * record the number of infrequent words
         */
        if (search.getWildSearch()) {
            asc.setSearchTokens(asc.getSearchWords());
        } else {
            asc.setSearchTokens(tokenizeArray(asc.getSearchWords(), asc.author.getCode(), 0, 0));
        }
        searchLog.add(new LogRow(LogLevel.TRACE, "\tSearch tokens: " + asc.printableSearchTokens()));

        int errorNum = asc.setLeastFrequentToken(authorIndex);

        if ((asc.getLeastFrequentToken() != null) && (errorNum % 4 < 2)) {
            // at least one searchable token and all tokens in author index

            // add all the references for the least frequent token to the referencesToSearch array
            asc.referencesToSearch = authorIndex.getReferences(asc.getLeastFrequentToken());

            // if there is more than one infrequent word
            // refine the number of references (combine if wild,
            // if not wild only use references where each word is found within 1 page
            if (asc.getNumInfrequentTokens() > 1) {

                // refine the references to search
                for (String token : asc.getInfrequentTokens()) {

                    if (!token.equals(asc.getLeastFrequentToken())) {
//                        search.setProgress("Refining references");
                        asc.referencesToSearch = search.refineReferences(authorIndex, token, asc.referencesToSearch);
                    }
                }

            } // end multiple search tokens

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

                    volumeSuccess = searchSingleVolume(resultText, asc);

                } // end one frequent token and all tokens found

            } // end had references to search
            else {
                searchLog.add(new LogRow(LogLevel.LOW, "No overlap in references for " + asc.author.getCode()));
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

    }

    private boolean searchSingleVolume(ArrayList<String> resultText, AuthorSearchCache asc) {
        // for each volume

        searchLog.add(new LogRow(LogLevel.TRACE, "\tVol: " + asc.volNum));

        int cPageNum = 0;
        final int cVolNum = asc.volNum;

        // get file name
        String filename = cfg.getResDir();
        if (asc.author.equals(Author.BIBLE)) {
            filename += asc.author.getTargetPath(BibleBook.values()[asc.volNum].getName() + ".htm");
        } else {
            filename += asc.author.getVolumePath(asc.volNum);
        }

        // read the file
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            // update progress
//            double authorOffset = (authorsToSearch.indexOf(author));
//            double authorProgess = ((double) volNum / author.getNumVols());
//            double progress = (fractionPerAuthor * authorOffset) + (fractionPerAuthor * authorProgess);
//            progress = (fractionPerAuthor * (authorsToSearch.indexOf(asc.author))) + (fractionPerAuthor * ((double) asc.volNum / asc.author.getNumVols()));
//            search.setProgress("Searching " + asc.author.getName() + " volume " + asc.volNum, progress);
            progress.addAndGet(1000 / asc.author.getNumVols());

            // line should not be null when first entering loop
            asc.line = br.readLine();

            while (asc.volNum == cVolNum) {
                // while still in the same volume
                // loop through references

                asc.prevLine = "";

                // skip to next page and get the last line of the previous page
                cPageNum = findNextPage(asc, br);

                // if the page number is 0 log the error and break out
                if (cPageNum == 0) {
                    searchLog.add(new LogRow(LogLevel.HIGH, "Could not find reference " + asc.author.getCode() + " " + asc.volNum + ":" + asc.pageNum));
                    return false;
                }

                // for each paragraph in the page
                // skip a line and check if the next line is a heading or paragraph
                br.readLine();
                asc.tempLine = br.readLine();
                if (asc.tempLine == null) {
                    searchLog.add(new LogRow(LogLevel.HIGH, "NULL line " + asc.author.getCode() + "vol " + asc.volNum + ":" + asc.pageNum));
                } else {
                    searchSinglePage(resultText, br, asc);
                }
                asc.line = asc.tempLine;

                // clear the previousLine
                asc.prevLine = "";

                // get the next reference
                asc.getNextPage();

            } // finished references in volume

        } catch (IOException ioe) {
            searchLog.add(new LogRow(LogLevel.HIGH, "Couldn't read " + asc.author.getTargetPath(filename) + " " + asc.volNum + ":" + asc.pageNum));
            return false;
        }

        return true;

    }

    private void searchSinglePage(ArrayList<String> resultText, BufferedReader br, AuthorSearchCache asc) throws IOException {

        boolean foundToken = false;

        // while still on the same page (class != page-number)
        while (asc.tempLine.contains("class=\"heading\"") || asc.tempLine.contains("class=\"paragraph\"") || asc.tempLine.contains("class=\"footnote\"")) {

            asc.line = br.readLine();

            ArrayList<String> stringsToSearch = new ArrayList<>();

            // get the searchLine based on the search scope
            if (search.getSearchScope() == SearchScope.SENTENCE) {
                stringsToSearch = convertLineIntoSentences(asc.line, getTrailingIncompleteSentence(asc.prevLine));
            } else {
                stringsToSearch.add(asc.line);
            }

            // search the scope
            foundToken = searchScope(resultText, stringsToSearch, asc, foundToken);

            // set the current line as the previous line if it is a paragraph
            if (asc.tempLine.contains("class=\"paragraph\"")) {
                asc.prevLine = asc.line;
            } else {
                asc.prevLine = "";
            }

            br.readLine();
            asc.tempLine = br.readLine();
        }

        if (!foundToken)
            searchLog.add(new LogRow(LogLevel.DEBUG, "Did not find token " + asc.author.getCode() + " " + asc.volNum + ":" + asc.pageNum));
    }

    private boolean searchScope(ArrayList<String> resultText, ArrayList<String> stringsToSearch, AuthorSearchCache asc, boolean foundToken) {

        for (String scope : stringsToSearch) {

            // if the current scope contains all search terms mark them and print it out (or one if it is a wildcard search)
            if (wordSearch(tokenizeLine(scope, asc.author.getCode(), asc.volNum, asc.pageNum), asc.getSearchTokens(), search.getWildSearch())) {

                foundToken = true;

                String markedLine = markLine(new StringBuilder(scope), asc.getSearchWords());

                // close any opened blockquote tags
                if (markedLine.contains("<blockquote>")) markedLine += "</blockquote>";

                resultText.add("\t<p>");
                resultText.add("\t\t<a href=\"..\\..\\" + asc.author.getTargetPath(asc.author.getCode() + asc.volNum + ".htm#" + asc.pageNum) + "\"> ");
                resultText.add(asc.author.getCode() + " volume " + asc.volNum + " page " + asc.pageNum + "</a> ");
                resultText.add(markedLine);
                resultText.add("\t</p>");

                search.incrementResults();
            }
        }

        return foundToken;
    }

    private int findNextPage(AuthorSearchCache asc, BufferedReader br) throws IOException {

        int cPageNum = 0;
        boolean foundPage = false;

        // clear the previous line
        asc.prevLine = "";

        // read until page number = page ref
        // or if page number is page before next reference get the last line
        while (!foundPage) {

            if (asc.line != null) {

                if (asc.line.contains("class=\"page-number\"")) {
                    asc.line = br.readLine();

                    try {

                        // get the current page's number
                        cPageNum = Integer.parseInt(asc.line.substring(asc.line.indexOf("=") + 1, asc.line.indexOf('>')));

                        // if it is the previous page
                        if (asc.pageNum == cPageNum + 1) {
                            asc.prevLine = getLastLineOfPage(br);

                            // set found page get page number
                            asc.line = br.readLine();

                            // skip any footnotes
                            while (asc.line.contains("class=\"footnote\"")) {
                                br.readLine();
                                br.readLine();
                                asc.line = br.readLine();
                            }

                            cPageNum = Integer.parseInt(asc.line.substring(asc.line.indexOf("=") + 1, asc.line.indexOf('>')));

                            if (asc.pageNum == cPageNum) {
                                return cPageNum;
                            } else {
                                // error next page not after previous page
                                searchLog.add(new LogRow(LogLevel.LOW, "Couldn't find search page: " + asc.author.getCode() + " " + asc.volNum + ":" + asc.pageNum));
                                return 0;
                            }
                        } else if (asc.pageNum == cPageNum) {
                            return cPageNum;
                        }
                    } catch (NumberFormatException nfe) {
                        searchLog.add(new LogRow(LogLevel.HIGH, "Error formatting page number in search: " + asc.author.getCode() + " " + asc.volNum + ":" + cPageNum));
                        return 0;
                    }
                }
            } else {
                searchLog.add(new LogRow(LogLevel.HIGH, "NULL line when reading " + asc.author.getCode() + " vol " + asc.volNum + " page " + asc.pageNum));
                break;
            }
            asc.line = br.readLine();

        } // found start of page

        // shouldn't reach here
        return 0;
    }

    private String getLastLineOfPage(BufferedReader br) throws IOException {

        // for each paragraph in the page
        // skip a line and check if the next line is a heading or paragraph
        br.readLine();

        String tempLine = br.readLine();
        String lastLine = "";

        // while not the last line
        while (tempLine.contains("class=\"heading\"") || tempLine.contains("class=\"paragraph\"") || tempLine.contains("class=\"footnote\"")) {

            // read the line of text
            lastLine = br.readLine();

            // skip the </div> tag line
            br.readLine();

            // read the next line class
            tempLine = br.readLine();
        }

        return lastLine;

    }

    int startOfLastSentencePos;

    private String getTrailingIncompleteSentence(String line) {

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
            if (!foundCurrentSearchToken && !search.getWildSearch()) return false;
        }

        // if it reaches this point as a wild search then no tokens were found
        if (wildSearch) return false;
        return true;
    }

    StringBuilder lineBuilder = new StringBuilder();
    int startHtml;
    int endHtml;

    private String[] tokenizeLine(String line, String authorCode, int volNum, int pageNum) {

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

        // split the line into tokens (words) by " " characters
        return tokenizeArray(line.split(" "), authorCode, volNum, pageNum);
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

    private String markLine(StringBuilder line, String[] words) {
        // highlight all the search words in the line with an html <mark/> tag

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
                line.replace(startOfWord, endOfWord, "<mark>" + currentCapitalisation + "</mark>");

                // set the char position to after the word
                charPos = endOfWord + 13;
            }
        }

        return line.toString();
    }

    @Override
    public ArrayList<LogRow> getLog() {
        return searchLog;
    }

    @Override
    public ArrayList<String> getResults() {
        return authorResults;
    }
}
