/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse;

import mse.common.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;

/**
 *
 * @author michael
 */
public class VolumeSearch implements Runnable {

    private final int TOO_FREQUENT = 1000;

    private String searchString;
    private Config cfg;
    //    private SearchScope searchScope;
    private ArrayList<Author> authorsToSearch;
    private Logger logger;

    public VolumeSearch(Config cfg, Logger logger, String searchString,
                        /*SearchScope searchScope,*/ ArrayList<Author> authorsToSearch) {
        this.cfg = cfg;
        this.logger = logger;
        this.searchString = searchString.toLowerCase();
//        this.searchScope = searchScope;
        this.authorsToSearch = authorsToSearch;
    }

    @Override
    public void run() {

        ArrayList<Byte> wordReferences = null;

        try {
            logger.log(LogLevel.DEBUG, "\tStarted Search: \"searchString\" in " + authorsToSearch.toString());

            // try to open and write to the results file
            try {
                PrintWriter pwResults = new PrintWriter(new File(cfg.getWorkingDir() + cfg.getResultsFileName()));

                // write the html header
                pwResults.println("<!DOCTYPE html>\n<html>\n\n<head>\n\t<title>Search Results</title>\n</head>\n");
                pwResults.println("<body>");
                pwResults.println("\t<p><img src=\"../images/results.gif\"></p>");

                // for each author to be searched
                for (Author nextAuthor : authorsToSearch) {

                    AuthorIndex authorIndex = new AuthorIndex(nextAuthor.getIndexFilePath());

                    logger.log(LogLevel.DEBUG, "\t\tSearching: " + nextAuthor.getName() + " for " + searchString);

                    pwResults.println("\t<p>\n\t\t<hr>\n\t\t<h1>Results of search through " + nextAuthor.getName() + "</h1>\n\t</p>");

                    // check if it is a wildcard search
                    boolean wildSearch = checkValidWildcardSearch(searchString);

                    // create and populate a list of words to search for
                    String searchWords = getSearchWords(authorIndex, searchString, wildSearch);

                    // print out the search words
                    String printableSearchWords = searchWords.replace(", ", ",");
                    pwResults.println(printableSearchWords);

                    // log the search strings
                    logger.log(LogLevel.DEBUG, "Search strings: " + printableSearchWords);

                    // get the list of words to search
                    String[] searchTokensList = searchWords.split(",");

                    /* Search all the words to make sure that all the search tokens are in the author's
                    * index. log any words that are too frequent, find the least frequent token and
                    * record the number of infrequent words */

                    boolean allTokensFound = true;
                    int lowestNumRefs = cfg.TOO_FREQUENT;
                    String leastFrequentToken = null;
                    int numInfreqTokens = 0;

                    for (String nextSearchToken : searchTokensList) {

                        // check that the index contains the words
                        Integer numReferences = authorIndex.getTokenCount(nextSearchToken);

                        if (numReferences != null) {

                            // check that the words aren't too frequent
                            if (numReferences > 0) {

                                if (numReferences < lowestNumRefs) {
                                    // lowest number of references so far
                                    lowestNumRefs = numReferences;
                                    leastFrequentToken = nextSearchToken;
                                }
                                numInfreqTokens++;

                            } else {
                                // word is too frequent
                                logger.log(LogLevel.INFO, "Token: " + nextSearchToken + " is too frequent");
                            }

                        } else {
                            // word not found in author index
                            logger.log(LogLevel.INFO, "Token: " + nextSearchToken + " not found in author " + authorIndex.getAuthorName());
                            allTokensFound = false;
                        }
                    }

                    if ((leastFrequentToken != null) && allTokensFound) {
                        // at least one searchable token and all tokens in author index

                        int currentVolume = 0;
                        int currentPage = 0;

                        ArrayList<String> referencesToSearch = new ArrayList<>(Arrays.asList(authorIndex.getReferences(leastFrequentToken)));

                        // if there is more than one infrequent word
                        // add the references to search to each word
                        if (numInfreqTokens > 1) {

                            // for each word
                            for (String token : searchTokensList) {

                                // if it's not the lowest word
                                if (!token.equals(leastFrequentToken)) {

                                    // if it has references in the index and it is infrequent
                                    String[] currentTokenRefs = authorIndex.getReferences(token);
                                    if ((currentTokenRefs != null) && (currentTokenRefs.length > 1)) {

                                        // if it is a wildcard search
                                        if (wildSearch) {

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

                                            ArrayList currentReferencesList = new ArrayList<String>(Arrays.asList(currentTokenRefs));
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


                            } // end iterating over each search token

                        } // end multiple search tokens

                        // TODO what is option.fullScan

                        // process each page that contains a match
                        for (String reference : referencesToSearch) {

                            // get the volume and page number from the reference
                            String[] refSplit = reference.split(":");
                            int vol = Integer.parseInt(refSplit[0]);
                            int page = Integer.parseInt(refSplit[1]);

                            String filename;

                            // get file name
                            if (nextAuthor.equals(Author.BIBLE)) {
                                filename = nextAuthor.getTargetPath(BibleBook.values()[vol].getName() + ".htm");
                            } else {
                                filename = nextAuthor.getVolumePath(vol);
                            }

                            // read the file
                            try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

                                //

                            } catch (IOException ioe) {
                                logger.log(LogLevel.HIGH, "Couldn't read " + nextAuthor.getTargetPath(filename));
                            }
                        }

//                            StringBuffer sbCurrentWordRefs = new StringBuffer();
//
//                                        htCompareWord.clear();
//                                        sbCurrentWordRefs.setLength(0);
//
//                                            int intCurrVol = 0;
//                                            int intCurrPage = 0;

//                                                int intCharPosLowest = 0;
//                                                int intCharPosCurr = 0;
//                                                String strCurrentWordRefs = sbCurrentWordRefs.toString();
//



//


//                                            } else {//not wildcard search
//                                                //check lowest array against this one
//                                                int intCharPos = 0;
//                                                String strRef = "";
//                                                String strVol = "";
//                                                int intPage = 0;
//                                                String strPreRef = "";
//                                                String strPostRef = "";
//                                                while ((intCharPos < strLowestWordRefs.length()) && (htCompareWord.size() > 0)) {
//                                                    strRef = strLowestWordRefs.substring(intCharPos, intCharPos
//                                                            + Constants.MAX_DIGITS_IN_VOL_NUM + Constants.MAX_DIGITS_IN_PAGE_NUM + 1);
//                                                    strVol = strRef.substring(0, Constants.MAX_DIGITS_IN_VOL_NUM + 1);
//                                                    intPage = Integer.parseInt(strRef.substring(Constants.MAX_DIGITS_IN_VOL_NUM + 1));
//                                                    strPreRef = strVol + Utils.leftZeroPad(intPage - 1, Constants.MAX_DIGITS_IN_PAGE_NUM);
//                                                    strPostRef = strVol + Utils.leftZeroPad(intPage + 1, Constants.MAX_DIGITS_IN_PAGE_NUM);
//
//                                                    if ((htCompareWord.get(strRef) == null)
//                                                            && (htCompareWord.get(strPreRef) == null)
//                                                            && (htCompareWord.get(strPostRef) == null)) {
//                                                        //ref is not page before/after ref in list of references for least frequent word
//                                                        //remove from sbLowestWordRefs
//                                                        strLowestWordRefs = strLowestWordRefs.substring(0, intCharPos)
//                                                                + strLowestWordRefs.substring(intCharPos
//                                                                + Constants.MAX_DIGITS_IN_VOL_NUM + Constants.MAX_DIGITS_IN_PAGE_NUM + 2);
//                                                    } else {
//                                                        intCharPos = intCharPos + Constants.MAX_DIGITS_IN_VOL_NUM + Constants.MAX_DIGITS_IN_PAGE_NUM + 2;
//                                                    }
//                                                }//each ref
//                                            }//not wildcard search



                    } // end one frequent token and all tokens found
                } // end searching each author

            } catch (IOException ioe) {
                // TODO error opening results file
            }

        } catch (Exception e) {
        }

    }


    private String getSearchWords(AuthorIndex authorIndex, String searchString, boolean wildsearch) {
        // this returns the search words as a comma separated list

        String searchWords = "";

        // check if the search is a wild search
        if (searchString.contains("*")) {
            if (wildsearch) {

                // remove the stars from the search string
                String bareSearchString = searchString.replace("*", "");

                for (String nextWord : authorIndex.getTokenCountMap().keySet()) {

                    if (nextWord.contains(bareSearchString)) {

                        // add the word to the list of words to be searched (with
                        // a comma if it isn't the first word
                        if (searchWords.length() > 0) {
                            searchWords += ",";
                        }
                        searchWords += nextWord;
                    }
                }
            } else {
                logger.log(LogLevel.INFO, "\t\t\tInvalid wildcard search: " + searchString);
            }
        } else {
            // if it's not a wildcard search
            searchWords += searchString.replace(" ", ",");
        }

        return searchWords;

    }

    private boolean checkValidWildcardSearch(String searchText) {
        ArrayList<Integer> starIndexes = new ArrayList<>();

        if (searchText.contains(" ")) {
            return false;
        }

        // get the index of each *
        for (int i = 0; i < searchString.length(); i++) {
            if (searchString.charAt(i) == '*') {
                starIndexes.add(i);
            }
        }

        // the stars can only be at the start and/or end of the search text
        if (starIndexes.size() == 2) {
            return (starIndexes.get(0) == 0) && (starIndexes.get(1) == searchText.length() - 1);
        } else if (starIndexes.size() == 1) {
            return ((starIndexes.get(0) == 0) || (starIndexes.get(0) == searchText.length() - 1));
        } else {
            return false;
        }

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

}
