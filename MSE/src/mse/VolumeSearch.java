/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse;

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
    private SearchScope searchScope;
    private ArrayList<Author> authorsToSearch;
    private Index index;

    public VolumeSearch(Config cfg, Index index, String searchString, SearchScope searchScope, ArrayList<Author> authorsToSearch) {
        this.cfg = cfg;
        this.index = index;
        this.searchString = searchString.toLowerCase();
        this.searchScope = searchScope;
        this.authorsToSearch = authorsToSearch;
    }

    @Override
    public void run() {
        
        ArrayList<Byte> wordReferences = null;
        
        try {
            cfg.writeToLog(" Began search for " + searchString + " in " + authorsToSearch.toString() + " at " + Calendar.getInstance());
            // try to open and write to the results file
            try {
                PrintWriter pwResults = new PrintWriter(new File(cfg.getWorkingDir() + cfg.getResultsFileName()));

                pwResults.println("<html><head><title>Search Results</title></head>");
                pwResults.println("<body bgcolor=\"#FFFFFF\" link=\"#0000FF\" vlink=\"#0000FF\" alink=\"#0000FF\">");
                pwResults.println("<center><img src=\"../images/results.gif\"></center>");

                // TODO progress window
//            for (int intAuth = 0; intAuth < Constants.MAX_AUTH_POS + 1; intAuth++) {
//                if (gui.cbToBeSearched[intAuth].isSelected()) {
//                    intAuthCount++;
//                }
//            }
//            float floatAuthsFactor = Constants.PROGRESS_BAR_MAX/(intAuthCount * 2);
                // TODO -I find out why bible and hymns are paragraph scope
                // for each author to be searched search the volume
                for (Author nextAuthor : authorsToSearch) {

                    cfg.writeToLog("Searching: " + nextAuthor.getName() + " for " + searchString);

                    // TODO -I find out what this does
                    HashMap<String, String> compareWord = new HashMap<>();

                    // initialise the search count
                    int lowCount = TOO_FREQUENT;
                    int lowIndex = -1;
                    int criteriaCount = 0;

                    boolean foundAll = true;
                    String basicSearchText = getBasicWords(searchString, true, true).trim();

                    // TODO -I stars?
                    int firstStarPos = searchString.indexOf("*");
                    int secondStarPos;

                    // TODO 3 update progress - searching for X
                    int loadingResult = index.loadAuthor(nextAuthor);
                    switch (loadingResult) {
                        case Index.LOADED:
                            cfg.writeToLog("Loaded " + nextAuthor.getName() + " index");
                            break;
                        case Index.RELOADED:
                            cfg.writeToLog("Reloaded " + nextAuthor.getName() + " index");
                            break;
                        case Index.NOT_LOADED:
//                            progressSearch.close();
                            cfg.writeToLog("Failed to load " + nextAuthor.getName() + " index");
                            break;
                    }

                    // TODO update progress - searching xAuthor ...
                    pwResults.println("<center><hr><h1><font color=\"#A00000\">Results of search through " + nextAuthor.getName() + "</font></h1></center>");

                    // initialise wildSearch to false and first wild word to true;
                    boolean wildSearch = false;
                    boolean firstWildWord = true;

                    // create and populate a list of words to search for
                    String searchWords = "";

                    // check if the search is a wild search
                    if (searchString.contains("*")) {
                        wildSearch = true;

                        if (checkValidWildcardSearch(searchString)) {

                            // remove the stars from the search string
                            String bareSearchString = searchString.replace("*", "");
                            for (String nextWord : index.getWordsList()) {
                                if (nextWord.contains(bareSearchString)) {
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
                            // TODO error - not a valid wild card search
                        }
                    } else {
                        // if it's not a wildcard search
                        searchWords += searchString.replace(" ", ",");
                    }
                    
                    // log the search strings
                    cfg.writeToLog("Search strings: " + searchWords);
                    
                    ArrayList<String> searchWordsList = new ArrayList<>(Arrays.asList(searchWords.split(",")));
                    
                    for (String nextSearchWord : searchWordsList) {
                        int intIndex = index.getIndexPos(searchWords);
                        if (intIndex != index.WORD_NOT_FOUND) {//found word in index
                            // TODO finish
//                            wordReferences = index.getRefs(intIndex);
//                            if (baRefs != null) {//infrequent word
//                                int intCurrCount = baRefs.length;
//                                if (intCurrCount < intLowCount) {//lowest so far
//                                    intLowCount = intCurrCount;
//                                    intLowIndex = intIndex;
//                                }//lowest so far
//                                
//                                // TODO -I what is the criteria count
////                                intCriteriaCount++;
//                            } else {//too frequent word
//                                cfg.writeToLog("'" + nextSearchWord + "' is too frequent");
//                            }
                        } else {//didn't find word in index
                            cfg.writeToLog("Couldn't find any occurrence of '" + nextSearchWord + "'");
                            foundAll = false;
                        }//didn't find word in index
                    } //each word in search string
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
