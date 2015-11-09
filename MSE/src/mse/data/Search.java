package mse.data;

import mse.common.*;

import java.util.ArrayList;

/**
 * Created by mj_pu_000 on 09/11/2015.
 */
public class Search {

    Config cfg;
    ILogger logger;
    String searchString;
    String[] searchWords;
    String[] searchTokens;
    boolean wildSearch;
    String leastFrequentToken;

    public Search(Config cfg, Logger logger, String searchString) {
        this.cfg = cfg;
        this.logger = logger;
        this.searchString = searchString;
    }

    public boolean getWildSearch() {
        return wildSearch;
    }

    public void setWildSearch() {

        ArrayList<Integer> starIndexes = new ArrayList<>();

        if (searchString.contains(" ")) {
            wildSearch = false;
        }

        // get the index of each *
        for (int i = 0; i < searchString.length(); i++) {
            if (searchString.charAt(i) == '*') {
                starIndexes.add(i);
            }
        }

        // the stars can only be at the start and/or end of the search text
        if (starIndexes.size() == 2) {
            wildSearch = (starIndexes.get(0) == 0) && (starIndexes.get(1) == searchString.length() - 1);
        } else if (starIndexes.size() == 1) {
            wildSearch = ((starIndexes.get(0) == 0) || (starIndexes.get(0) == searchString.length() - 1));
        } else {
            wildSearch = false;
        }

    }

    public String getLeastFrequentToken() {
        return leastFrequentToken;
    }

    public String[] getSearchWords() {
        return searchWords;
    }

    public void setSearchWords(AuthorIndex authorIndex) {
        // this sets the array of search words

        StringBuilder searchWordsBuilder = new StringBuilder();

        // check if the search is a wild search
        if (searchString.contains("*")) {
            if (wildSearch) {

                // remove the stars from the search string
                String bareSearchString = searchString.replace("*", "");

                for (String nextWord : authorIndex.getTokenCountMap().keySet()) {

                    if (nextWord.contains(bareSearchString)) {

                        // add the word to the list of words to be searched (with
                        // a comma if it isn't the first word
                        if (searchWordsBuilder.length() > 0) {
                            searchWordsBuilder.append(',');
                        }
                        searchWordsBuilder.append(nextWord);
                    }
                }

                searchWords = searchWordsBuilder.toString().split(",");
            } else {
                logger.log(LogLevel.INFO, "\t\t\tInvalid wildcard search: " + searchString);
            }
        } else {
            // if it's not a wildcard search
            searchWords = searchString.split(" ");
        }
    }

    public String[] getSearchTokens() {
        return searchTokens;
    }

    public void setSearchTokens(String[] searchTokens) {
        this.searchTokens = searchTokens;
    }

    public String printableSearchWords() {

        StringBuilder printableWords = new StringBuilder();

        for (String word : searchWords) {
            printableWords.append(printableWords + ", ");
        }

        return printableWords.toString().substring(0, printableWords.length() -1);
    }

    public int setLeastFrequentToken(AuthorIndex authorIndex) {
        // sets the least frequent token and returns the number of infrequent tokens found

        int lowestNumRefs = cfg.TOO_FREQUENT;
        int numInfrequentTokens = 0;

        // get the least frequent token and check that all the tokens have references
        for (String nextSearchToken : searchTokens) {

            // check that the index contains the words
            Integer numReferences = authorIndex.getTokenCount(nextSearchToken);

            if (numReferences != 0) {

                // check that the words aren't too frequent
                if (numReferences > 0) {

                    if (numReferences < lowestNumRefs) {
                        // lowest number of references so far
                        lowestNumRefs = numReferences;
                        leastFrequentToken = nextSearchToken;
                    }
                    numInfrequentTokens++;

                } else {
                    // word is too frequent
                    logger.log(LogLevel.INFO, "Token: " + nextSearchToken + " is too frequent");
                }

            } else {
                // word not found in author index
                logger.log(LogLevel.INFO, "Token: " + nextSearchToken + " not found in author " + authorIndex.getAuthorName());
            }
        }

        return numInfrequentTokens;
    }
}
