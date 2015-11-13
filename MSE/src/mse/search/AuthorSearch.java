/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse.search;

import mse.common.*;
import mse.data.Author;
import mse.data.AuthorIndex;
import mse.data.BibleBook;
import mse.data.Search;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author michael
 */
public class AuthorSearch extends Thread {

    private final int TOO_FREQUENT = 1000;

    private Config cfg;
    //    private SearchScope searchScope;
    private ArrayList<Author> authorsToSearch;
    private ILogger logger;
    private IndexStore indexStore;
    private Search search;

    // progress fraction per author
    private double fractionPerAuthor;
    private double progress;

    private ArrayList<String> stringsToSearch = new ArrayList<>();

    public AuthorSearch(Config cfg, ILogger logger, ArrayList<Author> authorsToSearch, IndexStore indexStore, Search search) {
        this.cfg = cfg;
        this.logger = logger;
//        this.searchScope = searchScope;
        this.authorsToSearch = authorsToSearch;
        this.indexStore = indexStore;
        this.search = search;
        this.fractionPerAuthor = (1.0 / authorsToSearch.size());
    }

    @Override
    public void run() {

        ArrayList<String> resultText = new ArrayList<>();

        logger.log(LogLevel.DEBUG, "\tStarted Search: \"" + search.getSearchString() + "\" in " + authorsToSearch.toString());



            logger.log(LogLevel.DEBUG, "\tOpened file: " + cfg.getResDir() + cfg.getResultsFileName());

           resultText.add(writeHtmlHeader());

            // for each author to be searched
            for (Author nextAuthor : authorsToSearch) {

                if (nextAuthor == Author.HYMNS) {
                    logger.log(LogLevel.LOW, "Hymns search doesn't work yet");
                    continue;
                }
                if (nextAuthor == Author.BIBLE) {
                    logger.log(LogLevel.LOW, "Bible search doesn't work yet");
                    continue;
                }
                searchAuthor(nextAuthor, resultText, search, indexStore);
                resultText.add("Number of results for " + nextAuthor.getName() + ": " + search.getNumAuthorResults());
                search.clearAuthorValues();

            } // end searching each author

            resultText.add("Number of total results: " + search.getNumTotalResults());

            resultText.add(writeHtmlFooter());

        // try to open and write to the results file
        File resultsFile = new File(cfg.getResDir() + cfg.getResultsFileName());
        if (!resultsFile.exists()) {
            resultsFile.getParentFile().mkdirs();
            try {
                resultsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (PrintWriter pwResults = new PrintWriter(resultsFile)) {

            resultText.forEach(pwResults::println);

        } catch (IOException ioe) {
            logger.log(LogLevel.HIGH, "Could not write to file: " + cfg.getResDir() + cfg.getResultsFileName());
        }

        search.setProgress("Done", 1.0);

        try {
            Desktop.getDesktop().open(new File(cfg.getResDir() + cfg.getResultsFileName()));
        } catch (IOException | IllegalArgumentException ioe) {
            logger.log(LogLevel.HIGH, "Could not open results file.");
        }

        logger.closeLog();

    }

    private void searchAuthor(Author author, ArrayList<String> resultText, Search search, IndexStore indexStore) {

        // get a new search cache
        AuthorSearchCache asc = new AuthorSearchCache();

        // get the author index
        search.setProgress("Loading index for " + author.getName());
        AuthorIndex authorIndex = indexStore.getIndex(author, logger);

        logger.log(LogLevel.DEBUG, "\tSearching: " + author.getName() + " for \"" + search.getSearchString() + "\"");

        // get the search words
        search.setSearchWords(authorIndex);

        // print the title of the author search results and search words
        resultText.add("\n\t<hr>\n\t<h1>Results of search through " + author.getName() + "</h1>");
        resultText.add("\n\t<p>\n\t\tSearched: " + search.printableSearchWords() + "\n\t</p>");
        logger.log(LogLevel.TRACE, "\tSearch strings: " + search.printableSearchWords());

        /* Search all the words to make sure that all the search tokens are in the author's
         * index. log any words that are too frequent, find the least frequent token and
         * record the number of infrequent words
         */
        search.setSearchTokens(tokenizeArray(search.getSearchWords(), author.getCode(), 0, 0));
        logger.log(LogLevel.TRACE, "\tSearch tokens: " + search.printableSearchTokens());

        boolean foundAllTokens = search.setLeastFrequentToken(authorIndex);

        if ((search.getLeastFrequentToken() != null) && (foundAllTokens)) {
            // at least one searchable token and all tokens in author index

            // add all the for the least frequent token to the references to search array
            short[] referencesToSearchArray = authorIndex.getReferences(search.getLeastFrequentToken());
            asc.referencesToSearch = new ArrayList<>(referencesToSearchArray.length);
            for (short reference : referencesToSearchArray) asc.referencesToSearch.add(reference);

            // if there is more than one infrequent word
            // refine the number of references (combine if wild,
            // if not wild only use references where each word is found within 1 page
            if (search.getNumInfrequentTokens() > 1) {

                // refine the references to search
                for (String token : search.getSearchTokens()) {

                    if (!token.equals(search.getLeastFrequentToken())) {
                        asc.referencesToSearch = refineReferences(authorIndex, token, asc.referencesToSearch);
                    }
                }
            } // end multiple search tokens

            // TODO what is option.fullScan

            // if there are still any references to search

            // should be at least two references (volume and page to start)
            if (asc.referencesToSearch.size() > 1) {

                // process each page that contains a match
                asc.refIndex = 0;

                // first two references should be volume and page
                asc.getNextRef();
                asc.getNextRef();

                // for each reference
                while (asc.refIndex < asc.referencesToSearch.size()) {

                    searchSingleVolume(resultText, author, asc);

                } // end one frequent token and all tokens found

            } // end had references to search
            else {
                logger.log(LogLevel.LOW, "No overlap in references for " + author.getCode());
            }

        } else {
            if (search.getLeastFrequentToken() == null) {
                resultText.add("Search words appeared too frequently.");
                logger.log(LogLevel.LOW, "\tToo frequent tokens in " + author.getCode() + ": " + search.printableSearchTokens());
            }
            if (!foundAllTokens) {
                resultText.add("Not all words were found in index.");
                logger.log(LogLevel.LOW, "\tTokens in " + author.getCode() + " not all found: " + search.printableSearchTokens());
            }
        }

    }

    private ArrayList<Short> refineReferences(AuthorIndex authorIndex, String token, ArrayList<Short> referencesToSearch) {

        ArrayList<Short> newListOfReferences = new ArrayList<>();

        // current volume, page number and reference for "Current Ref To Search" and "Current Extra Reference"
        short crtsVolNum = 0;
        short crtsPageNum = 0;
        short crts;

        short cefVolNum = 0;
        short cefPageNum = 0;
        short cef;

        // compare the references of each word to find matches
        // currentRefIndex -> referencesToSearchIndex
        // extraRefIndex -> currentTokenReferencesIndex
        int crtsIndex;
        int cefIndex;


        // if it has references in the index and it is infrequent
        short[] extraTokenRefs = authorIndex.getReferences(token);
        if ((extraTokenRefs != null) && (extraTokenRefs.length > 1)) {

            // if it is a wildcard search
            if (search.getWildSearch()) {

                crtsIndex = 0;
                cefIndex = 0;

                // add any references in the current references list
                // to the list of references to search
                while ((crtsIndex < referencesToSearch.size()) &&
                        (cefIndex < extraTokenRefs.length)) {

                    // get the next reference of the current and extra references
                    crts = referencesToSearch.get(crtsIndex);
                    cef = extraTokenRefs[cefIndex];

                    // interpret the references
                    if (crts < 0) {
                        // if the next reference is negative it is a new volume
                        crtsVolNum = crts;
                    } else {
                        // if the next reference is positive it is a new page
                        crtsPageNum = crts;
                    }

                    if (cef < 0) {
                        // as above
                        cefVolNum = cef;
                    } else {
                        cefPageNum = cef;
                    }

                    // if the volume number is zero then error
                    if (crtsVolNum == 0 || cefVolNum == 0) logger.log(LogLevel.HIGH, "Invalid references " + authorIndex.getAuthorName());

                    // add the reference that is closest to the beginning of the author
                    // only add same references once

                    if (crtsVolNum < cefVolNum) {
                        // ref to search volume number is larger (more negative) so add cef
                        newListOfReferences.add(cef);
                        cefIndex++;
                    } else if (cefVolNum < crtsVolNum) {
                        // reverse of above
                        newListOfReferences.add(crts);
                        crtsIndex++;
                    } else if (crtsPageNum < cefPageNum) {
                        // volume numbers are same and ref to search page is smaller so add ref to search
                        newListOfReferences.add(crts);
                        crtsIndex++;
                    } else if (cefPageNum < crtsPageNum) {
                        // // reverse of above
                        newListOfReferences.add(cef);
                        cefIndex++;
                    } else {
                        // volume and page number are same add single reference
                        newListOfReferences.add(crts);
                        crtsIndex++;
                        cef++;
                    }

                }

                // if there are any references left in the current list of references add
                // them to the list of references to search
                while ((cefIndex < extraTokenRefs.length)) {
                    newListOfReferences.add(extraTokenRefs[cefIndex]);
                    cefIndex++;
                } // end combining list of references

            } else {
                // not a wildcard search

                crtsIndex = 0;
                cefIndex = 0;

                // discard all references to search where the currentTokenRefs does not contain a ref
                // with a page adjacent to each ref in referencesToSearch
                while ((crtsIndex < referencesToSearch.size()) && (cefIndex < extraTokenRefs.length)) {

                    crts = referencesToSearch.get(crtsIndex);
                    cef = extraTokenRefs[cefIndex];

                    // interpret the references
                    if (crts < 0) {
                        // if the next reference is negative it is a new volume
                        crtsVolNum = crts;
                    } else {
                        // if the next reference is positive it is a new page
                        crtsPageNum = crts;
                    }

                    if (cef < 0) {
                        // as above
                        cefVolNum = cef;
                    } else {
                        cefPageNum = cef;
                    }

                    // if the volume number is zero then error
                    if (crtsVolNum == 0 || cefVolNum == 0) logger.log(LogLevel.HIGH, "Invalid references " + authorIndex.getAuthorName());

                    // if on the same volume reference add it
                    // if in the same volume and on adjacent pages

                    if (crtsVolNum < cefVolNum) {
                        // the crts Volume is ahead (more negative) increment cef
                        cefIndex++;
                    } else if (cefVolNum < crtsVolNum) {
                        // reverse of above
                        crts++;
                    } else if (crts < 0) {
                        // crts is a volume number and the volume numbers are equal so add the
                        // volume number and increment both
                        newListOfReferences.add(crts);
                        crtsIndex++;
                        cefIndex++;
                    } else if (checkAdjacent(crtsPageNum, cefPageNum)){
                        // volume numbers are equal, they are pointing at pages and they are adjacent
                        // add the crts and increment crts (next crts page may be adjacent to
                        // current cef but not next cef)
                        newListOfReferences.add(crts);
                        crtsIndex++;
                    } else if (crts < cef) {
                        // in same volume, both are page numbers, not adjacent and crts is
                        // closer to start of volume so increment crts
                        crtsIndex++;
                    } else {
                        // as above but cef is closer to start of volume
                        cefIndex++;
                    }

                } // end checking each reference to be searched

            } // end not wildcard search

        } // end word has refs

        return newListOfReferences;
    }

    private boolean checkAdjacent(short a, short b) {
        return a == b || (a+1) == b || a == (b+1);
    }

    private void searchSingleVolume(ArrayList<String> resultText, Author author, AuthorSearchCache asc) {
        // for each volume

        int cPageNum = 0;
        final int cVolNum = asc.volNum;

        // get file name
        String filename = cfg.getResDir();
        if (author.equals(Author.BIBLE)) {
            filename += author.getTargetPath(BibleBook.values()[asc.volNum].getName() + ".htm");
        } else {
            filename += author.getVolumePath(asc.volNum);
        }

        boolean foundPage;

        // read the file
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            // update progress
//            double authorOffset = (authorsToSearch.indexOf(author));
//            double authorProgess = ((double) volNum / author.getNumVols());
//            double progress = (fractionPerAuthor * authorOffset) + (fractionPerAuthor * authorProgess);
            progress = (fractionPerAuthor * (authorsToSearch.indexOf(author))) + (fractionPerAuthor * ((double) asc.volNum / author.getNumVols()));
            search.setProgress("Searching " + author.getName() + " volume " + asc.volNum, progress);

            // line should not be null when first entering loop
            asc.line = br.readLine();

            while (asc.refIndex < asc.referencesToSearch.size() && asc.nextRef > 0) {
                // while still in the same volume
                // loop through references

                foundPage = false;
                asc.prevLine = "";

                // read until page number = page ref
                // or if page number is page before next reference get the last line
                while (!foundPage) {
                    if (asc.line != null) {
                        if (asc.line.contains("class=\"page-number\"")) {
                            asc.line = br.readLine();
                            try {
                                cPageNum = Integer.parseInt(asc.line.substring(asc.line.indexOf("=") + 1, asc.line.indexOf('>')));
                                if (asc.pageNum == cPageNum) foundPage = true;

                                if (cPageNum == 377) {
                                    System.out.println("debug");
                                }

                                // if it is the previous page
                                if (asc.pageNum == cPageNum  -1) {
                                    asc.prevLine = getLastLineOfPage(br);

                                    // set found page get page number
                                    asc.line = br.readLine();

                                    // if the line is a footnote then skip it
                                    if (asc.line.contains("class=\"footnote\"")) {
                                        br.readLine();
                                        br.readLine();
                                        asc.line = br.readLine();
                                    }

                                    cPageNum = Integer.parseInt(asc.line.substring(asc.line.indexOf("=") + 1, asc.line.indexOf('>')));
                                    foundPage = true;
                                }
                            } catch (NumberFormatException nfe) {
                                logger.log(LogLevel.HIGH, "Error formatting page number in search: " + author.getCode() + " " + asc.volNum + ":" + cPageNum);
                                return;
                            }
                        }
                    } else {
                        logger.log(LogLevel.HIGH, "NULL line when reading " + author.getCode() + " vol " + asc.volNum + " page " + asc.pageNum);
                        break;
                    }
                    if (!foundPage) asc.line = br.readLine();
                } // found start of page

                // for each paragraph in the page
                // skip a line and check if the next line is a heading or paragraph
                br.readLine();
                asc.tempLine = br.readLine();
                if (asc.tempLine == null) {
                    logger.log(LogLevel.HIGH, "NULL line " + author.getCode() + "vol " + asc.volNum + ":" + asc.pageNum);
                } else {
                    searchSinglePage(resultText, br, author, asc);
                }
                asc.line = asc.tempLine;

                // clear the previousLine
                asc.prevLine = "";

                // keep reading references until a new page in same volume
                while (asc.refIndex < asc.referencesToSearch.size() && asc.volNum == cVolNum && asc.pageNum == cPageNum) {
                    asc.getNextRef();
                }

            } // finished references in volume

        } catch (IOException ioe) {
            logger.log(LogLevel.HIGH, "Couldn't read " + author.getTargetPath(filename) + asc.volNum + ":" + asc.pageNum);
        }

    }

    private void searchSinglePage(ArrayList<String> resultText, BufferedReader br, Author author, AuthorSearchCache asc) throws IOException {

        // while still on the same page (class != page-number)
        while (asc.tempLine.contains("class=\"heading\"") || asc.tempLine.contains("class=\"paragraph\"")) {

            asc.line = br.readLine();

            stringsToSearch.clear();

            // get the searchLine based on the search scope
            if (search.getSearchScope() == SearchScope.SENTENCE) {
                stringsToSearch = convertLineIntoSentences(asc.line, getTrailingIncompleteSentence(asc.prevLine));
            } else {
                stringsToSearch.add(asc.line);
            }

            // for each string to search
            for (String scope : stringsToSearch) {

                // if the current scope contains all search terms mark them and print it out
                if (wordSearch(tokenizeLine(scope, author.getCode(), asc.volNum, asc.pageNum), search.getSearchTokens())) {

                    String markedLine = markLine(new StringBuilder(scope), search.getSearchWords());

                    resultText.add("\t<p>");
                    resultText.add("\t\t<a href=\"..\\..\\" + author.getTargetPath(author.getCode() + asc.volNum + ".htm#" + asc.pageNum) + "\"> ");
                    resultText.add(author.getCode() + " volume " + asc.volNum + " page " + asc.pageNum + "</a> ");
                    resultText.add(markedLine);
                    resultText.add("\t</p>");

                    search.incrementResults();
                }
            }

            // set the current line as the previous line if it is a paragraph
            if (asc.tempLine.contains("class=\"paragraph\"")) {
                asc.prevLine = asc.line;
            } else {
                asc.prevLine = "";
            }

            br.readLine();
            asc.tempLine = br.readLine();
        }
    }

    private String getLastLineOfPage(BufferedReader br) throws IOException {

        // for each paragraph in the page
        // skip a line and check if the next line is a heading or paragraph
        br.readLine();

        String tempLine = br.readLine();
        String lastLine = "";

        // while not the last line
        while (tempLine.contains("class=\"heading\"") || tempLine.contains("class=\"paragraph\"")) {

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
        if (startOfLastSentencePos <0) return line;

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

        while (endOfSentencePos >= 0 && endOfSentencePos < line.length()) {

            endOfSentencePos = line.indexOf("<a name=", startOfSentencePos) -1;

            if (endOfSentencePos < 0) continue;



            sentences.add(line.substring(startOfSentencePos, endOfSentencePos));

            // skip the a tags
            startOfSentencePos = line.indexOf('>', endOfSentencePos) + 1;
            startOfSentencePos = line.indexOf('>', startOfSentencePos) + 1;
            endOfSentencePos = startOfSentencePos;

        }

        if (sentences.size() > 0) sentences.set(0, trailingIncompleteSentence + " " + sentences.get(0));

        return sentences;
    }

    private int[] ref = new int[2];

    boolean foundCurrentSearchToken;

    private boolean wordSearch(String[] currentLineTokens, String[] searchTokens) {


        for (String nextSearchToken : searchTokens) {
            foundCurrentSearchToken = false;
            for (String nextLineToken : currentLineTokens) {
                if (nextSearchToken.equals(nextLineToken)) {
                    foundCurrentSearchToken = true;
                }
            }
            if (!foundCurrentSearchToken) return false;
        }
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
                logger.log(LogLevel.HIGH, "Error processing token " + authorCode + " " + volNum + ":" + pageNum + ": " + token);
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
            if(!Character.isLetter(c)) {
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

    private String writeHtmlHeader() {
        return "<!DOCTYPE html>" +
                "\n\n<html>" +
                "\n\n<head>" +
                "\n\t<link rel=\"stylesheet\" type=\"text/css\" href=\"../../mseStyle.css\" />" +
                "\n\t<title>Search Results</title>" +
                "\n</head>" +
                "\n<body>" +
                "\t<p><img src=\"../../img/results.gif\"></p>";
    }

    private String writeHtmlFooter() {
        return "\n</body>\n\n</html>";
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
                if (startOfWord >= 0 && Character.isLetter(line.charAt(startOfWord -1))) continue;
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

}
