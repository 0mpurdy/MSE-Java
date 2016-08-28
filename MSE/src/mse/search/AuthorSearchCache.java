package mse.search;

import java.util.ArrayList;

import mse.common.config.Config;
import mse.common.log.LogRow;
import mse.data.author.Author;
import mse.data.author.AuthorIndex;
import mse.data.search.Reference;
import mse.data.search.Search;
import mse.data.search.SearchType;
import mse.helpers.HtmlHelper;

/**
 * Cache for storing search parameters
 * @author michaelpurdy
 */
public class AuthorSearchCache {

    private Config cfg;

    // public variables are a bad idea but if it works it is faster
    public AuthorIndex authorIndex;
    public Author author;

    public Reference reference;
    public BibleResultsLogic brl;

    // region search values

    public short[] referencesToSearch;
    public int refIndex;
    private String searchString;
    private boolean wildSearch;
    private String[] searchWords;
    private String[] searchTokens;
    private ArrayList<String> infrequentTokens;
    private String leastFrequentToken;
    private ArrayList<String> tooFrequentTokens;
    private int numInfrequentTokens;
    public int numAuthorResults;
    public short nextRef;
    public String line;
    public String currentSectionHeader;
    public String prevLine;
    private SearchType searchType;
    public boolean notFoundCurrentHymnBook;
    boolean notFoundToken = false;

    // endregion

    public AuthorSearchCache(Config cfg, AuthorIndex authorIndex, Search search) {
        this.cfg = cfg;
        this.authorIndex = authorIndex;
        this.author = authorIndex.getAuthor();
        this.leastFrequentToken = null;
        this.numAuthorResults = 0;

        this.searchString = search.getSearchString();
        this.wildSearch = search.getWildSearch();

        this.searchType = search.getSearchType();

        this.brl = new BibleResultsLogic();
        this.reference = new Reference(author, 0,0,0,0);

        tooFrequentTokens = new ArrayList<>();

        infrequentTokens = new ArrayList<>();
    }

    // region setup

    public void setup(ArrayList<LogRow> searchLog) {
        setSearchWords();
        setSearchTokens(searchLog);
        setLeastFrequentToken();
    }

    private void setSearchWords() {
        // this sets the array of search words

        StringBuilder searchWordsBuilder = new StringBuilder();

        // check if the search is a wild search
        if (searchString.contains("*")) {
            if (wildSearch) {

                // 0 = at start
                // 1 = at end
                // 2 = contains
                int type;

                // check type of wildcard search
                if (searchString.charAt(0) == '*') {
                    if (searchString.charAt(searchString.length() - 1) == '*') {
                        type = 2;
                    } else {
                        type = 1;
                    }
                } else {
                    type = 0;
                }

                // remove the stars from the search string
                String wildToken = searchString.replace("*", "");
                wildToken = wildToken.toUpperCase();

                for (String nextWord : authorIndex.getTokenCountMap().keySet()) {

                    if (wildWordCheck(type, wildToken, nextWord)) {

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
            searchWords = searchString.split(" ");
        }
    }

    private void setSearchTokens(ArrayList<LogRow> searchLog) {
        if (getWildSearch()) {
            this.searchTokens = getSearchWords();
        } else {
            this.searchTokens = HtmlHelper.tokenizeLine(getSearchString());
        }
    }

    private int setLeastFrequentToken() {
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
                    tooFrequentTokens.add(nextSearchToken);
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

    // endregion

    // region increments

    public void getNextPage() {
        if (refIndex >= referencesToSearch.length) {
            reference.volNum = 0;
            reference.pageNum = 0;
            return;
        }

        nextRef = referencesToSearch[refIndex++];

        if (nextRef < 0) {
            reference.volNum = -nextRef;
            nextRef = referencesToSearch[refIndex++];
            reference.pageNum = nextRef;
        } else {
            reference.pageNum = nextRef;
        }
    }

    public void incrementResults() {
        if (author != Author.BIBLE) {
            numAuthorResults++;
        } else if (brl.searchingDarby || !brl.foundDarby) {
            numAuthorResults++;
        }
    }

    public void incrementSectionNumber() {
        switch (author) {
            case BIBLE:
                reference.sectionNum += brl.verseNumIncrement();
                break;
            default:
                reference.sectionNum++;
        }
    }

    // endregion

    // region checks

    private boolean wildWordCheck(int type, String wildToken, String word) {
        return ((type == 0 && word.startsWith(wildToken)) ||
                type == 1 && word.endsWith(wildToken) ||
                type == 2 && word.contains(wildToken));
    }

    private boolean checkAdjacent(short a, short b) {
        return a == b || (a + 1) == b || a == (b + 1);
    }

    // endregion

    // region refine

    public void setupReferences() {
        setReferencesToSearch();
        refineReferences();
        refIndex = 0;
    }

    private void setReferencesToSearch() {
        // add all the references for the least frequent token to the referencesToSearch array
        referencesToSearch = authorIndex.getReferences(getLeastFrequentToken());
    }

    private void refineReferences() {
        // if there is more than one infrequent word
        // refine the number of references (combine if wild,
        // if not wild only use references where each word is found within 1 page

        if (numInfrequentTokens > 1) {

            // refine the references to search
            for (String token : infrequentTokens) {

                if (!token.equals(leastFrequentToken)) {
//                        search.setProgress("Refining references");
                    referencesToSearch = refineSingleToken(token);
                }
            }

        } // end multiple search tokens
    }

    private short[] refineSingleToken(String token) {
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
//                    if (crtsVolNum == 0 || cerVolNum == 0)
                    // TODO re-add logging
//                        logger.log(LogLevel.HIGH, "Invalid references " + authorIndex.getAuthorName());

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

                    // TODO re-add logging
                    // if the volume number is zero then error
//                    if (crtsVolNum == 0 || cerVolNum == 0)
//                        logger.log(LogLevel.HIGH, "Invalid references " + authorIndex.getAuthorName());

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

    // endregion

    // region publicGetters

    public String getSearchString() {
        return searchString;
    }

    public String getAuthorName() {
        return author.getName();
    }

    public boolean getWildSearch() {
        return wildSearch;
    }

    public String getTooFrequentTokensList() {
        boolean first =true;
        String list = "";
        for (String token : tooFrequentTokens) {

            if (first) {
                first = false;
            } else {
                list += ", ";
            }

            list += token;
        }

        return list;
    }

    public ArrayList<String> getTooFrequentTokens() {
        return tooFrequentTokens;
    }

    public boolean getTokenNotFound() {
        return notFoundToken;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public String getLeastFrequentToken() {
        return leastFrequentToken;
    }

    public String[] getSearchWords() {
        return searchWords;
    }

    public String[] getSearchTokens() {
        return searchTokens;
    }

    // endregion

    // region printables

    public String printableSearchWords() {
        return HtmlHelper.printableArray(searchWords);
    }

    public String printableSearchTokens() {
        return HtmlHelper.printableArray(searchTokens);
    }

    // endregion

}
