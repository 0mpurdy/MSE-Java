package mse.search;

import mse.common.Config;
import mse.common.LogLevel;
import mse.data.Author;
import mse.data.AuthorIndex;
import mse.data.Search;

import java.util.ArrayList;

/**
 * Created by mj_pu_000 on 12/11/2015.
 */
public class AuthorSearchCache {

    private Config cfg;

    // public variables are a bad idea but if it works it is faster
    public AuthorIndex authorIndex;
    public Author author;

    public short[] referencesToSearch;
    public int refIndex;

    private Search search;

    private String[] searchWords;
    private String[] searchTokens;

    private ArrayList<String> infrequentTokens;
    private String leastFrequentToken;

    private String tooFrequentTokens;

    private int numInfrequentTokens;

    public int numAuthorResults;

    public int volNum;
    public int pageNum;

    public short nextRef;

    public String line;
    public String tempLine;
    public String prevLine;

    public AuthorSearchCache(Config cfg, AuthorIndex authorIndex, Search search) {
        this.cfg = cfg;
        this.authorIndex = authorIndex;
        this.author = authorIndex.getAuthor();
        this.search = search;
        this.leastFrequentToken = null;
        this.numAuthorResults = 0;

        tooFrequentTokens = "";

        infrequentTokens = new ArrayList<>();
    }

    public void getNextPage() {
        if (refIndex >= referencesToSearch.length) {
            volNum = 0;
            pageNum = 0;
            return;
        }

        nextRef = referencesToSearch[refIndex++];

        if (nextRef < 0) {
            volNum = -nextRef;
            nextRef = referencesToSearch[refIndex++];
            pageNum = nextRef;
        } else {
            pageNum = nextRef;
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
        if (search.getSearchString().contains("*")) {
            if (search.getWildSearch()) {

                // remove the stars from the search string
                String bareSearchString = search.getSearchString().replace("*", "");
                bareSearchString = bareSearchString.toUpperCase();

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
//                logger.log(LogLevel.INFO, "\t\t\tInvalid wildcard search: " + searchString);
            }
        } else {
            // if it's not a wildcard search
            searchWords = search.getSearchString().split(" ");
        }
    }

    public String[] getSearchTokens() {
        return searchTokens;
    }

    public void setSearchTokens(String[] searchTokens) {
        this.searchTokens = searchTokens;
    }

    public String printableSearchWords() {
        return printableArray(searchWords);
    }

    public String printableSearchTokens() {
        return printableArray(searchTokens);
    }

    public String printableArray(String[] array) {
        StringBuilder printableArray = new StringBuilder();

        for (String word : array) {
            printableArray.append(word).append(", ");
        }

        if (printableArray.length() < 2) return "";

        // remove last comma
        return printableArray.toString().substring(0, printableArray.length() - 2);
    }

    public String getTooFrequentTokens() {
        return tooFrequentTokens;
    }

    public int setLeastFrequentToken(AuthorIndex authorIndex) {
        // sets the least frequent token and returns the number of infrequent tokens found

        boolean foundToken = false;
        boolean tooFrequent = false;
        boolean notFound = false;

        /* returns:
                1 : all tokens found (no errors)
                2 : all tokens not found
                3 : some tokens not found
                4 : all tokens too frequent
                5 : some tokens too frequent
                6 : all tokens not found or too frequent
                7 : some tokens not found and some tokens too frequent
         */

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

                    // found at least one token
                    foundToken = true;

                } else {
                    // word is too frequent
                    // TODO add in logging
//                    logger.log(LogLevel.DEBUG, "\tToken: " + nextSearchToken + " is too frequent");
                    if (tooFrequent) tooFrequentTokens += ", ";
                    tooFrequentTokens += nextSearchToken;
                    tooFrequent = true;
                }

            } else {
                // word not found in author index
//                logger.log(LogLevel.DEBUG, "Token: " + nextSearchToken + " not found in author " + authorIndex.getAuthorName());
                notFound = true;
            }
        }

        int errorNum = 0;
        if (!foundToken) errorNum = 1;
        if (notFound) errorNum += 2;
        if (tooFrequent) errorNum += 4;

        return errorNum;
    }

    public ArrayList<String> getInfrequentTokens() {
        return infrequentTokens;
    }

    public int getNumInfrequentTokens() {
        return numInfrequentTokens;
    }
}
