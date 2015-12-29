package mse.data;

import mse.common.*;
import mse.search.SearchType;

import java.util.ArrayList;

/**
 * Created by mj_pu_000 on 09/11/2015.
 */
public class Search {

    private Config cfg;
    private ILogger logger;
    private String searchString;

    private boolean wildSearch;

    private SearchType searchType;

    private int numTotalResults;

    public Search(Config cfg, ILogger logger, String searchString) {
        this.cfg = cfg;
        this.logger = logger;
        this.searchString = searchString;
        this.searchType = cfg.getSearchType();
        this.numTotalResults = 0;
        setWildSearch();
    }

    public boolean getWildSearch() {
        return wildSearch;
    }

    private void setWildSearch() {

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
        } else
            wildSearch = starIndexes.size() == 1 && ((starIndexes.get(0) == 0) || (starIndexes.get(0) == searchString.length() - 1));

    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public int getTotalSearchResults() {
        return numTotalResults;
    }

    public void addAuthorSearchResults(int authorResults) {
        numTotalResults += authorResults;
    }
}
