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

    private boolean wildSearch;

    private SearchScope searchScope;
    private int numTotalResults;

    public Search(Config cfg, ILogger logger, String searchString) {
        this.cfg = cfg;
        this.logger = logger;
        this.searchString = searchString;
        this.numTotalResults = 0;
    }

    public Search(Search oldSearch) {
        this.cfg = oldSearch.cfg;
        this.logger = oldSearch.logger;
        this.searchString = oldSearch.searchString;
        this.numTotalResults = 0;
    }

    public void incrementResults() {
        numTotalResults++;
    }

    public short[] refineReferences(AuthorIndex authorIndex, String token, short[] referencesToSearch) {

        ArrayList<Short> newListOfReferences = new ArrayList<>();

        // current volume, page number and reference for "Current Ref To Search" and "Current Extra Reference"
        short crtsVolNum = 0;
        short crtsPageNum = 0;
        short crts;

        short cerVolNum = 0;
        short cerPageNum = 0;
        short cer;

        // compare the references of each word to find matches
        // currentRefIndex -> referencesToSearchIndex
        // extraRefIndex -> currentTokenReferencesIndex
        int crtsIndex;
        int cerIndex;

        // if it has references in the index and it is infrequent
        short[] extraTokenRefs = authorIndex.getReferences(token);
        if ((extraTokenRefs != null) && (extraTokenRefs.length > 1)) {

            // if it is a wildcard search
            if (wildSearch) {

                crtsIndex = 0;
                cerIndex = 0;

                // add any references in the current references list
                // to the list of references to search
                while ((crtsIndex < referencesToSearch.length) &&
                        (cerIndex < extraTokenRefs.length)) {

                    // get the next reference of the current and extra references
                    crts = referencesToSearch[crtsIndex];
                    cer = extraTokenRefs[cerIndex];

                    // interpret the references
                    if (crts < 0) {
                        // if the next reference is negative it is a new volume
                        crtsVolNum = crts;
                    } else {
                        // if the next reference is positive it is a new page
                        crtsPageNum = crts;
                    }

                    if (cer < 0) {
                        // as above
                        cerVolNum = cer;
                    } else {
                        cerPageNum = cer;
                    }

                    // if the volume number is zero then error
                    if (crtsVolNum == 0 || cerVolNum == 0)
                        logger.log(LogLevel.HIGH, "Invalid references " + authorIndex.getAuthorName());

                    // add the reference that is closest to the beginning of the author
                    // only add same references once

                    if (crtsVolNum < cerVolNum) {
                        // ref to search volume number is larger (more negative) so add cer
                        newListOfReferences.add(cer);
                        cerIndex++;
                    } else if (cerVolNum < crtsVolNum) {
                        // reverse of above
                        newListOfReferences.add(crts);
                        crtsIndex++;
                    } else if (crts < 0) {
                        // volume number is the same add once and inc both
                        newListOfReferences.add(crts);
                        crtsIndex++;
                        cerIndex++;
                    } else if (crtsPageNum < cerPageNum) {
                        // volume numbers are same and ref to search page is smaller so add ref to search
                        newListOfReferences.add(crts);
                        crtsIndex++;
                    } else if (cerPageNum < crtsPageNum) {
                        // // reverse of above
                        newListOfReferences.add(cer);
                        cerIndex++;
                    } else {
                        // volume and page number are same add single reference
                        newListOfReferences.add(crts);
                        crtsIndex++;
                        cerIndex++;
                    }

                }

                // if there are any references left in the current list of references add
                // them to the list of references to search
                while ((cerIndex < extraTokenRefs.length)) {
                    newListOfReferences.add(extraTokenRefs[cerIndex]);
                    cerIndex++;
                } // end combining list of references

                // if there are any references left in the current list of references add
                // them to the list of references to search
                while ((crtsIndex < referencesToSearch.length)) {
                    newListOfReferences.add(referencesToSearch[crtsIndex]);
                    crtsIndex++;
                } // end combining list of references

            } else {
                // not a wildcard search

                crtsIndex = 0;
                cerIndex = 0;

                boolean recordVolNum = false;

                // discard all references to search where the currentTokenRefs does not contain a ref
                // with a page adjacent to each ref in referencesToSearch
                while ((crtsIndex < referencesToSearch.length) && (cerIndex < extraTokenRefs.length)) {

                    crts = referencesToSearch[crtsIndex];
                    cer = extraTokenRefs[cerIndex];

                    // interpret the references
                    if (crts < 0) {
                        // if the next reference is negative it is a new volume
                        crtsVolNum = crts;
                    } else {
                        // if the next reference is positive it is a new page
                        crtsPageNum = crts;
                    }

                    if (cer < 0) {
                        // as above
                        cerVolNum = cer;
                    } else {
                        cerPageNum = cer;
                    }

                    // if the volume number is zero then error
                    if (crtsVolNum == 0 || cerVolNum == 0)
                        logger.log(LogLevel.HIGH, "Invalid references " + authorIndex.getAuthorName());

                    // if on the same volume reference add it
                    // if in the same volume and on adjacent pages

                    if (crtsVolNum < cerVolNum) {
                        // the crts Volume is ahead (more negative) increment cef
                        cerIndex++;
                    } else if (cerVolNum < crtsVolNum) {
                        // reverse of above
                        crtsIndex++;
                    } else if (crts < 0) {
                        // crts is a volume number and the volume numbers are equal so
                        // increment both
                        recordVolNum = true;
                        crtsIndex++;
                        cerIndex++;
                    } else if (checkAdjacent(crtsPageNum, cerPageNum)) {
                        // volume numbers are equal, they are pointing at pages and they are adjacent
                        // add the crts and increment crts (next crts page may be adjacent to
                        // current cef but not next cef)
                        if (recordVolNum) {
                            newListOfReferences.add(crtsVolNum);
                            recordVolNum = false;
                        }
                        newListOfReferences.add(crts);
                        crtsIndex++;
                    } else if (crts < cer) {
                        // in same volume, both are page numbers, not adjacent and crts is
                        // closer to start of volume so increment crts
                        crtsIndex++;
                    } else {
                        // as above but cef is closer to start of volume
                        cerIndex++;
                    }

                } // end checking each reference to be searched

            } // end not wildcard search

        } // end word has refs

        short[] newReferencesArray = new short[newListOfReferences.size()];
        int i = 0;
        for (short newReference : newListOfReferences) newReferencesArray[i++] = newReference;

        return newReferencesArray;
    }

    private boolean checkAdjacent(short a, short b) {
        return a == b || (a + 1) == b || a == (b + 1);
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
        } else
            wildSearch = starIndexes.size() == 1 && ((starIndexes.get(0) == 0) || (starIndexes.get(0) == searchString.length() - 1));

    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public SearchScope getSearchScope() {
        return searchScope;
    }

    public void setSearchScope(SearchScope searchScope) {
        this.searchScope = searchScope;
    }
}
