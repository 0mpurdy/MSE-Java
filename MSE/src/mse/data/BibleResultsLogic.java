package mse.data;

/**
 * Created by Michael Purdy on 30/12/2015.
 */
public class BibleResultsLogic {

    public boolean writtenBibleSearchTableHeader;
    public boolean writtenBibleSearchTableFooter;
    public boolean searchingDarby;
    public boolean foundDarby;

    public BibleResultsLogic() {
        this.writtenBibleSearchTableHeader = false;
        this.writtenBibleSearchTableFooter = false;
        this.searchingDarby = true;
        this.foundDarby = false;
    }

    public void setFoundDarby(boolean foundToken) {
        if (searchingDarby) foundDarby = foundToken;
    }

    public String previousDarbyLine;

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

    public int verseNumIncrement() {

        if (searchingDarby) return 1;
        else return 0;
    }

}
