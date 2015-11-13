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
        nextRef = referencesToSearch[refIndex++];

        if (nextRef < 0) {
            volNum = nextRef * -1;
            nextRef = referencesToSearch[refIndex++];
            pageNum = nextRef;
        } else {
            pageNum = nextRef;
        }
    }

}
