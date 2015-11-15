package mse.search;

import mse.data.Author;

import java.util.ArrayList;

/**
 * Created by mj_pu_000 on 12/11/2015.
 */
public class AuthorSearchCache {

    // public variables are a bad idea but if it works it is faster
    public Author author;

    public short[] referencesToSearch;
    public int refIndex;

    public int volNum;
    public int pageNum;

    public short nextRef;

    public String line;
    public String tempLine;
    public String prevLine;

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

}
