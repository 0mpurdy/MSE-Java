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

    private String searchString;

    private boolean wildSearch;

    private boolean writtenBibleSearchTableHeader;
    private boolean writtenBibleSearchTableFooter;
    private boolean searchingDarby;
    public String previousDarbyLine;

    private int bibleVerseNum;

    String[] searchWords;
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
    public String currentSectionHeader;
    public String prevLine;
    private SearchScope searchScope;

    public AuthorSearchCache(Config cfg, AuthorIndex authorIndex, Search search) {
        this.cfg = cfg;
        this.authorIndex = authorIndex;
        this.author = authorIndex.getAuthor();
        this.leastFrequentToken = null;
        this.numAuthorResults = 0;

        this.searchString = search.getSearchString();
        this.wildSearch = search.getWildSearch();

        this.searchScope = search.getSearchScope();

        this.writtenBibleSearchTableHeader = false;
        this.writtenBibleSearchTableFooter = false;
        this.searchingDarby = true;

        this.bibleVerseNum = 0;

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

    public String getAuthorCode() {
        return author.getCode();
    }

    public String getLeastFrequentToken() {
        return leastFrequentToken;
    }

    public String[] getSearchWords() {
        return searchWords;
    }

    public void setSearchWords() {
        // this sets the array of search words

        StringBuilder searchWordsBuilder = new StringBuilder();

        // check if the search is a wild search
        if (searchString.contains("*")) {
            if (wildSearch) {

                // remove the stars from the search string
                String bareSearchString = searchString.replace("*", "");
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

    public int setLeastFrequentToken() {
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

    public String getSearchString() {
        return searchString;
    }

    public String getAuthorName() {
        return author.getName();
    }

    public boolean getWildSearch() {
        return wildSearch;
    }

    public void setReferencesToSearch() {
        referencesToSearch = authorIndex.getReferences(getLeastFrequentToken());
    }

    public void refineReferences() {
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

    public short[] refineSingleToken(String token) {
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

    private boolean checkAdjacent(short a, short b) {
        return a == b || (a + 1) == b || a == (b + 1);
    }

    public SearchScope getSearchScope() {
        return searchScope;
    }

    public void incrementResults() {
        numAuthorResults++;
    }

    public boolean isWrittenBibleSearchTableHeader() {
        return writtenBibleSearchTableHeader;
    }

    public boolean isWrittenBibleSearchTableFooter() {
        return writtenBibleSearchTableFooter;
    }

    public void setWrittenBibleSearchTableHeader(boolean writtenBibleSearchTableHeader) {
        this.writtenBibleSearchTableHeader = writtenBibleSearchTableHeader;
    }

    public void setWrittenBibleSearchTableFooter(boolean writtenBibleSearchTableFooter) {
        this.writtenBibleSearchTableFooter = writtenBibleSearchTableFooter;
    }

    public boolean isSearchingDarby() {
        return searchingDarby;
    }

    public ArrayList<String> finishSearchingSingleBibleScope(String line, ArrayList<String> resultText, boolean foundToken) {

        if (writtenBibleSearchTableHeader && !searchingDarby) {

            if (!foundToken) {
                resultText.add("<td>" + removeHtml(line) + "</td>");
            }

            resultText.add("\t\t</tr>");
            resultText.add("\t</table>");

            writtenBibleSearchTableHeader = false;
        } else if (searchingDarby && !writtenBibleSearchTableHeader) previousDarbyLine = line;

        searchingDarby = !searchingDarby;

        return resultText;
    }

    public int getBibleVerseNum() {
        return bibleVerseNum;
    }

    public void setBibleVerseNum(int bibleVerseNum) {
        this.bibleVerseNum = bibleVerseNum;
    }

    private String removeHtml(String line) {
        return removeHtml(new StringBuilder(line)).toString();
    }

    private StringBuilder removeHtml(StringBuilder line) {
        int charPos = 0;

        while (++charPos < line.length()) {
            if (line.charAt(charPos) == '<') {
                int tempCharIndex = charPos + 1;
                while (tempCharIndex < line.length() - 1 && line.charAt(tempCharIndex) != '>') tempCharIndex++;
                tempCharIndex++;
                line.replace(charPos, tempCharIndex, "");
            }
        }

        return line;
    }

    public void incrementVerseNum() {

        if (searchingDarby) bibleVerseNum++;
    }
}
