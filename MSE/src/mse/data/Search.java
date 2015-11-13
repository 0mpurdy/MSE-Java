package mse.data;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import mse.common.*;
import mse.search.SearchScope;

import java.util.ArrayList;

/**
 * Created by mj_pu_000 on 09/11/2015.
 */
public class Search {

    private Config cfg;
    private ILogger logger;
    private String searchString;

    private String[] searchWords;
    private String[] searchTokens;

    private ArrayList<String> infrequentTokens;
    private String leastFrequentToken;

    private boolean wildSearch;

    private SearchScope searchScope;

    private ProgressBar progressBar;
    private Label progressLabel;

    private int numInfrequentTokens;

    private int numAuthorResults;
    private int numTotalResults;

    public Search(Config cfg, ILogger logger, String searchString, ProgressBar progressBar, Label progressLabel) {
        this.cfg = cfg;
        this.logger = logger;
        this.searchString = searchString;
        this.progressBar = progressBar;
        this.progressLabel = progressLabel;
        this.leastFrequentToken = null;
        this.numAuthorResults = 0;
        this.numTotalResults = 0;

        infrequentTokens = new ArrayList<>();
    }

    public void clearAuthorValues() {
        searchWords = new String[0];
        searchTokens = new String[0];
        infrequentTokens.clear();
        leastFrequentToken = null;
        numInfrequentTokens = 0;
        numAuthorResults = 0;
    }

    public void incrementResults() {
        numAuthorResults++;
        numTotalResults++;
    }

    public int getNumAuthorResults() {
        return numAuthorResults;
    }

    public int getNumTotalResults() {
        return numTotalResults;
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

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
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
            printableWords.append(word).append(", ");
        }

        return printableWords.toString().substring(0, printableWords.length() -2);
    }

    public String printableSearchTokens() {

        StringBuilder printableTokens = new StringBuilder();

        for (String word : searchTokens) {
            printableTokens.append(word).append(", ");
        }

        if (printableTokens.length() < 2) return "";
        return printableTokens.toString().substring(0, printableTokens.length() -2);
    }

    public boolean setLeastFrequentToken(AuthorIndex authorIndex) {
        // sets the least frequent token and returns the number of infrequent tokens found

        int lowestNumRefs = cfg.TOO_FREQUENT;

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
                    infrequentTokens.add(nextSearchToken);
                    numInfrequentTokens++;

                } else {
                    // word is too frequent
                    logger.log(LogLevel.DEBUG, "\tToken: " + nextSearchToken + " is too frequent");
                }

            } else {
                // word not found in author index
                logger.log(LogLevel.DEBUG, "Token: " + nextSearchToken + " not found in author " + authorIndex.getAuthorName());
                return false;
            }
        }

        return true;
    }

    public int getNumInfrequentTokens() {
        return numInfrequentTokens;
    }

    public SearchScope getSearchScope() {
        return searchScope;
    }

    public void setSearchScope(SearchScope searchScope) {
        this.searchScope = searchScope;
    }

    public void setProgress(double progress) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress(progress);
            }
        });
    }

    public void setProgress(String message, double progress) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                progressLabel.setText(message);
                progressBar.setProgress(progress);
            }
        });
    }

    public void setProgress(String message) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                progressLabel.setText(message);
            }
        });
    }
}
