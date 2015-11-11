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
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author michael
 */
public class AuthorSearch extends Thread {

    private final String[] deleteChars = {"?","\"","!",",",".","-","\'",":",
            "1","2","3","4","5","6","7","8","9","0",";","@",")","(","�","*","[","]","\u00AC","{","}","\u2019", "~",
            "\u201D","\u00A6","…","†","&","`","$","\u00A7","|","\t","=","+","\u2018","\u20AC","/","\u00B6","_","�","�","�","�","%","#"};

    private final int TOO_FREQUENT = 1000;

    private String searchString;
    private Config cfg;
    //    private SearchScope searchScope;
    private ArrayList<Author> authorsToSearch;
    private Logger logger;
    private IndexStore indexStore;
    private Search search;

    // progress fraction per author
    private double fractionPerAuthor;
    private double progress;

    public AuthorSearch(Config cfg, Logger logger, String searchString, ArrayList<Author> authorsToSearch, IndexStore indexStore, Search search) {
        this.cfg = cfg;
        this.logger = logger;
        this.searchString = searchString.toLowerCase();
//        this.searchScope = searchScope;
        this.authorsToSearch = authorsToSearch;
        this.indexStore = indexStore;
        this.search = search;
        this.fractionPerAuthor = (1.0 / authorsToSearch.size());
    }

    @Override
    public void run() {

        logger.log(LogLevel.DEBUG, "\tStarted Search: \"" + searchString+ "\" in " + authorsToSearch.toString());

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

            logger.log(LogLevel.DEBUG, "\tOpened file: " + cfg.getResDir() + cfg.getResultsFileName());

            writeHtmlHeader(pwResults);

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
                searchAuthor(nextAuthor, pwResults, search, indexStore);
                pwResults.println("Number of results for " + nextAuthor.getName() + ": " + search.getNumAuthorResults());
                search.clearAuthorValues();

            } // end searching each author

            pwResults.println("Number of total results: " + search.getNumTotalResults());

            writeHtmlFooter(pwResults);

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

    private void searchAuthor(Author author, PrintWriter pw, Search search, IndexStore indexStore) {

        // get the author index
        search.setProgress("Loading index for " + author.getName());
        AuthorIndex authorIndex = indexStore.getIndex(author, logger);

        logger.log(LogLevel.DEBUG, "\tSearching: " + author.getName() + " for \"" + searchString + "\"");

        // get the search words
        search.setSearchWords(authorIndex);

        // print the title of the author search results and search words
        pw.println("\n\t<hr>\n\t<h1>Results of search through " + author.getName() + "</h1>");
        pw.println("\n\t<p>\n\t\tSearched: " + search.printableSearchWords() + "\n\t</p>");
        logger.log(LogLevel.TRACE, "\tSearch strings: " + search.printableSearchWords());

        /* Search all the words to make sure that all the search tokens are in the author's
         * index. log any words that are too frequent, find the least frequent token and
         * record the number of infrequent words
         */
        search.setSearchTokens(tokenizeArray(search.getSearchWords(), author.getCode(), 0, 0));
        logger.log(LogLevel.TRACE, "\tSearch tokens: " + search.printableSearchTokens());
        logger.flush();

        boolean foundAllTokens = search.setLeastFrequentToken(authorIndex);

        if ((search.getLeastFrequentToken() != null) && (foundAllTokens)) {
            // at least one searchable token and all tokens in author index

            String[] referencesToSearchArray = authorIndex.getReferences(search.getLeastFrequentToken());
            ArrayList<String> referencesToSearch = new ArrayList<>(Arrays.asList(referencesToSearchArray));

            // if there is more than one infrequent word
            // refine the number of references (combine if wild,
            // if not wild only use references where each word is found within 1 page
            if (search.getNumInfrequentTokens() > 1) {

                // refine the references to search
                referencesToSearch = refineReferences(authorIndex, search, referencesToSearch, search.getLeastFrequentToken());

            } // end multiple search tokens

            // TODO what is option.fullScan

            // if there are still any references to search

            if (referencesToSearch.size() > 0) {

                int volNum;

                // process each page that contains a match
                int refIndex = 0;

                // read the first reference
                int[] nextRef = getReference(referencesToSearch, refIndex);
                refIndex++;

                // for each reference
                while (refIndex < referencesToSearch.size() && nextRef != null) {

                    volNum = nextRef[0];

                    refIndex = searchSingleVolume(pw, author, nextRef, volNum, referencesToSearch, refIndex);

                } // end one frequent token and all tokens found

            } // end had references to search
            else {
                logger.log(LogLevel.LOW, "No overlap in references for " + author.getCode());
            }

        } else {
            if (search.getLeastFrequentToken() == null) {
                pw.println("Search words appeared too frequently.");
                logger.log(LogLevel.LOW, "\tToo frequent tokens in " + author.getCode() + ": " + search.printableSearchTokens());
            }
            if (!foundAllTokens) {
                pw.println("Not all words were found in index.");
                logger.log(LogLevel.LOW, "\tTokens in " + author.getCode() + " not all found: " + search.printableSearchTokens());
            }
        }

    }

    private ArrayList<String> refineReferences(AuthorIndex authorIndex, Search search, ArrayList<String> referencesToSearch, String leastFrequentToken) {

        ArrayList currentReferencesList;

        // for each word
        for (String token : search.getSearchTokens()) {

            // if it's not the lowest word
            if (!token.equals(leastFrequentToken)) {

                // if it has references in the index and it is infrequent
                String[] currentTokenRefs = authorIndex.getReferences(token);
                if ((currentTokenRefs != null) && (currentTokenRefs.length > 1)) {

                    // if it is a wildcard search
                    if (search.getWildSearch()) {

                        // compare the references of each word to find matches
                        // rtsIndex -> referencesToSearchIndex
                        // ctrIndex -> currentTokenReferencesIndex
                        int rtsIndex = 0;
                        int ctrIndex = 0;

                        // add any references in the current references list
                        // to the list of references to search
                        while ((rtsIndex < referencesToSearch.size()) &&
                                (ctrIndex < currentTokenRefs.length)) {

                            String nextCurrentReference = currentTokenRefs[ctrIndex];

                            // compare the next two references
                            int compareValue = referencesToSearch.get(rtsIndex).compareTo(nextCurrentReference);

                            // if references are equal increment both indexes
                            if (compareValue == 0) {
                                rtsIndex++;
                                ctrIndex++;
                            } else if (compareValue >0) {
                                // if the reference is not already in the list of references to search add it
                                referencesToSearch.add(nextCurrentReference);
                                ctrIndex++;
                            } else {
                                // reference is in refs to search but not current word refs
                                rtsIndex++;
                            }

                        }

                        // if there are any references left in the current list of references add
                        // them to the list of references to search
                        while ((ctrIndex < currentTokenRefs.length)) {
                            referencesToSearch.add(currentTokenRefs[ctrIndex]);
                            ctrIndex++;
                        } // end combining list of references

                    } else {
                        // not a wildcard search

                        currentReferencesList = new ArrayList<String>(Arrays.asList(currentTokenRefs));
                        int rtsIndex = 0;

                        // remove all references to search where the currentTokenRefs does not contain a ref
                        // with a page adjacent to each ref in referencesToSearch
                        while ((rtsIndex < referencesToSearch.size()) && (currentReferencesList.size() > 0)) { // TODO is second size check necessary

                            final String currentReferenceToBeSearched = referencesToSearch.get(rtsIndex);

                            // split the current reference (currentReferenceSplit - crs)

                            String[] crs = currentReferenceToBeSearched.split(":");
                            int currentRefPageNum = Integer.parseInt(crs[1]);
                            String previousPage = crs[0] + ":" + (currentRefPageNum - 1);
                            String nextPage = crs[0] + ":" + (currentRefPageNum + 1);

                            // if the next reference for the current word is not on an
                            // adjacent page remove the reference from the list of
                            // references to search
                            if ((!(currentReferencesList.contains(crs)))
                                    && (!(currentReferencesList.contains(previousPage)))
                                    && (!(currentReferencesList.contains(nextPage)))) {
                                referencesToSearch.remove(currentReferenceToBeSearched);
                            } else {
                                // move on to next reference
                                rtsIndex++;
                            }

                        } // end checking each reference to be searched

                    } // end not wildcard search

                } // end word has refs

            } // end word not least frequent

        } // end iterating over each search token

        return referencesToSearch;
    }

    private int searchSingleVolume(PrintWriter pw, Author author, int[] nextRef, int volNum, ArrayList<String> referencesToSearch, int refIndex) {
        // for each volume

        // get file name
        String filename = cfg.getResDir();
        if (author.equals(Author.BIBLE)) {
            filename += author.getTargetPath(BibleBook.values()[nextRef[0]].getName() + ".htm");
        } else {
            filename += author.getVolumePath(nextRef[0]);
        }

        int pageNum = 0;
        boolean foundPage;

        // read the file
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            // update progress
//            double authorOffset = (authorsToSearch.indexOf(author));
//            double authorProgess = ((double) volNum / author.getNumVols());
//            double progress = (fractionPerAuthor * authorOffset) + (fractionPerAuthor * authorProgess);
            progress = (fractionPerAuthor * (authorsToSearch.indexOf(author))) + (fractionPerAuthor * ((double) volNum / author.getNumVols()));
            search.setProgress("Searching " + author.getName() + " volume " + volNum, progress);

            // line should not be null when first entering loop
            String line = br.readLine();

            while (refIndex < referencesToSearch.size() && nextRef[0] == volNum) {
                // while still in the same volume

                foundPage = false;

                // read until page number = page ref
                while (!foundPage) {
                    if (line != null) {
                        if (line.contains("class=\"page-number\"")) {
                            line = br.readLine();
                            try {
                                pageNum = Integer.parseInt(line.substring(line.indexOf("=") + 1, line.indexOf('>')));
                                if (pageNum == nextRef[1]) foundPage = true;
                            } catch (NumberFormatException nfe) {
                                logger.log(LogLevel.HIGH, "Error formatting page number in search: " + author.getCode() + " " + volNum + ":" + pageNum);
                                return -1;
                            }
                        }
                    } else {
                        logger.log(LogLevel.HIGH, "NULL line when reading " + author.getCode() + " vol " + nextRef[0] + " page " + nextRef[1]);
                        break;
                    }
                    if (!foundPage) line = br.readLine();
                } // found start of page

                // TODO add scope

                // for each paragraph in the page
                // skip a line and check if the next line is a heading or paragraph
                br.readLine();
                String tempLine = br.readLine();
                if (tempLine == null) {
                    logger.log(LogLevel.HIGH, "NULL line " + author.getCode() + "vol " + volNum + ":" + pageNum);
                } else {
                    while (tempLine.contains("class=\"heading\"") || tempLine.contains("class=\"paragraph\"")) {

                        line = br.readLine();

                        // if the current scope contains all search terms mark them and print it out
                        if (wordSearch(tokenizeLine(line, author.getCode(), volNum, pageNum), search.getSearchTokens())) {

                            String markedLine = markLine(new StringBuilder(line), search.getSearchWords());

                            pw.println("\t<p>");
                            pw.print("\t\t<a href=\"..\\..\\" + author.getTargetPath(author.getCode() + volNum + ".htm#" + pageNum) + "\"> ");
                            pw.print(author.getCode() + " volume " + volNum + " page " + pageNum + "</a> ");
                            pw.println(markedLine);
                            pw.println("\t</p>");

                            search.incrementResults();
                        }

                        br.readLine();
                        tempLine = br.readLine();
                    }
                }
                line = tempLine;

                // keep reading references until a new page
                while (refIndex < referencesToSearch.size() && volNum == nextRef[0] && pageNum == nextRef[1]) {
                    nextRef = getReference(referencesToSearch, refIndex);
                    refIndex++;
                }

            } // finished references in volume

        } catch (IOException ioe) {
            logger.log(LogLevel.HIGH, "Couldn't read " + author.getTargetPath(filename) + volNum + ":" + pageNum);
        }

        return refIndex;
    }

    ArrayList<String> sentences = new ArrayList<>();
    int startOfSentencePos;
    int endOfSentencePos;

    private ArrayList<String> convertLineIntoScentences(String line) {

        sentences.clear();
        startOfSentencePos = 0;
        endOfSentencePos = 0;

        while (endOfSentencePos < line.length()) {

            endOfSentencePos = line.indexOf("<name=") -1;

            if (endOfSentencePos < 0) continue;

            sentences.add(line.substring(startOfSentencePos, endOfSentencePos));

            startOfSentencePos = line.indexOf('>', endOfSentencePos) + 1;
            endOfSentencePos = startOfSentencePos;

        }

        return sentences;
    }

    private int[] ref = new int[2];

    private int[] getReference(ArrayList<String> referencesToSearch, int refIndex) {
        // returns reference in the form [volume number, page number]
        String[] refString = referencesToSearch.get(refIndex).split(":");
        ref[0] = Integer.parseInt(refString[0]);
        ref[1] = Integer.parseInt(refString[1]);
        return ref;
    }

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
                token = processUncommonString(token);
                if (!isAlpha(token)) {
                    logger.log(LogLevel.HIGH, "Error processing token " + authorCode + " " + volNum + ":" + pageNum + ": " + token);
                    token = "";
                }
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

//        for (String c : deleteChars) {
//            if (token.contains(c)) {
//                token = token.replace(c, "");
//                return token;
//            }
//        }
        return token;
    }

    private String processUncommonString(String token) {
        for (String c : deleteChars) {
            token = token.replace(c, "");
        }
        return token;
    }

    private void writeHtmlHeader(PrintWriter pw) {
        pw.println("<!DOCTYPE html>\n<link rel=\"stylesheet\" type=\"text/css\" href=\"../../mseStyle.css\">\n\n<html>\n\n<head>\n\t<title>Search Results</title>\n</head>\n");
        pw.println("<body>");
        pw.println("\t<p><img src=\"../../img/results.gif\"></p>");
    }

    private void writeHtmlFooter(PrintWriter pw) {
        pw.println("\n</body>\n\n</html>");
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

    private String markLine(StringBuilder line, String[] words) {
        // highlight all the search words in the line with an html <mark/> tag

        for (String word : words) {

            charPos = 0;

            // while there are still more words matching the current word in the line
            // and the char position hasn't exceeded the line
            while (charPos < line.length() && ((startOfWord = line.indexOf(word, charPos)) != -1)) {

                endOfWord = startOfWord + word.length();
                charPos = endOfWord;

                // if the word has a letter before it or after it then skip it
                if (startOfWord >= 0 && Character.isLetter(line.charAt(startOfWord -1))) continue;
                if (endOfWord < line.length() && Character.isLetter(line.charAt(endOfWord))) continue;

                // otherwise mark the word
                line.replace(startOfWord, endOfWord, "<mark>" + word + "</mark>");

                // set the char position to after the word
                charPos = endOfWord + 13;
            }
        }

        return line.toString();
    }

}
