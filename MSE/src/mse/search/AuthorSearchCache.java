package mse.search;

import java.util.ArrayList;

/**
 * Created by mj_pu_000 on 12/11/2015.
 */
public class AuthorSearchCache {

    // public variables are a bad idea but if it works it is faster
    public ArrayList<Short> referencesToSearch;
    public int refIndex;

    public int volNum;
    public int pageNum;

    public short nextRef;

    public String line;
    public String tempLine;
    public String prevLine;

    public void getNextRef() {
        nextRef = referencesToSearch.get(refIndex);

        if (nextRef < 0) {
            volNum = nextRef * -1;
        } else {
            pageNum = nextRef;
        }
        refIndex++;
    }

}
