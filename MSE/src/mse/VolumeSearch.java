/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse;

import mse.common.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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

                    // create and populate a list of words to search for
                    String searchWords = "";

                    // check if the search is a wild search
                    if (searchString.contains("*")) {
                        if (checkValidWildcardSearch(searchString)) {

                            boolean firstWildWord = true;

                            // remove the stars from the search string
                            String bareSearchString = searchString.replace("*", "");

                            for (String nextWord : authorIndex.getTokenCountMap().keySet()) {

                                if (nextWord.contains(bareSearchString)) {

                                    // TODO if first wild word then first search word? combine ifs?

                                    // for every word that contains the search string
                                    // print out the word to the user (with a preceding
                                    // comma if it isn't the first word)

                                    if (firstWildWord) {
                                        firstWildWord = false;
                                        pwResults.println(nextWord);
                                    } else {
                                        pwResults.println(", " + nextWord);
                                    }

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
                    
                    // log the search strings
                    logger.log(LogLevel.DEBUG, "Search strings: " + searchWords);

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

                        for (String reference : authorIndex.getReferences(leastFrequentToken)) {

                        }


                    }

//                        StringBuffer sbLowestWordRefs = new StringBuffer();
//                        String strPage = "";
//                        baRefs = index.getRefs(intLowIndex);
//                        if (baRefs != null) {//infrequent word - should always be true
//                            int intCurrVol = 0;
//                            int intCurrPage = 0;
//                            for (int x = 0; x <= baRefs.length - 1; x++) {//each reference
//                                int byteCurr = intFromByte(baRefs[x]);
//                                if (byteCurr == 0) {//new volume
//                                    x++;
//                                    intCurrVol = intFromByte(baRefs[x]);
//                                    intCurrPage = 0;
//                                } else {//reference
//                                    if (byteCurr == 255) {//large delta
//                                        x++;
//                                        int intHigh = intFromByte(baRefs[x]);
//                                        x++;
//                                        int intLow = intFromByte(baRefs[x]);
//                                        intCurrPage = intCurrPage + (intHigh * 254) + intLow;
//                                    } else {//small delta
//                                        intCurrPage = intCurrPage + byteCurr;
//                                    }//small delta
//                                    sbLowestWordRefs.append(Utils.leftZeroPad(intCurrVol, Constants.MAX_DIGITS_IN_VOL_NUM) + "/" +
//                                            Utils.leftZeroPad(intCurrPage, Constants.MAX_DIGITS_IN_PAGE_NUM) + " ");
//                                }//reference
//                            }//each reference
//                        }//infrequent word - should always be true
//                        progressSearch.updateStatus("Built index for: " + index.saWords[intLowIndex]);
//                        log.write(Constants.LOG_HIGH, "<br>References to " + index.saWords[intLowIndex] + " (least frequent): " + sbLowestWordRefs.toString());
//
//                        strLowestWordRefs = sbLowestWordRefs.toString();


                }

            } catch (IOException ioe) {
                // TODO error opening results file
            }

        } catch (Exception e) {
        }

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
