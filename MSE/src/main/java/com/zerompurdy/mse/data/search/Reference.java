package com.zerompurdy.mse.data.search;

import com.zerompurdy.mse.data.author.Author;
import com.zerompurdy.mse.data.author.BibleBook;
import com.zerompurdy.mse.data.author.HymnBook;
import com.zerompurdy.mse.helpers.LinkHelper;

/**
 * A reference to a search result
 *
 * @author michaelpurdy
 */
public class Reference {

    private Author author;

    // public allows faster access
    public int volNum, pageNum, sectionNum, sentenceNum;

    public Reference(Author author, int volNum, int pageNum, int sectionNum, int sentenceNum) {
        this.author = author;
        this.volNum = volNum;
        this.pageNum = pageNum;
        this.sectionNum = sectionNum;
        this.sentenceNum = sentenceNum;
    }

    public Reference copy() {
        return new Reference(author, volNum, pageNum, sectionNum, sentenceNum);
    }

    /**
     * Get a readable representation of the reference
     *
     * @return readable representation of the reference
     */
    public String getReadableReference() {
        switch (author) {
            case BIBLE:
                return BibleBook.values()[volNum - 1].getNameWithSpaces() + " chapter " + pageNum + ":" + sectionNum;
            case HYMNS:
                return Integer.toString(pageNum);
            default:
                return author.getName() + " volume " + volNum + " page " + pageNum;
        }
    }

    /**
     * Get a short readable representation of the reference
     * eg "FER vol 4:170"
     * eg "2 Timothy 2:2"
     * eg "1973 Hymn book 480:1"
     *
     * @return short readable representation of the reference
     */
    public String getShortReadableReference() {
        if (author.isMinistry()) {
            return author.getCode() + "vol " + volNum + ":" + pageNum;
        } else if (author.equals(Author.BIBLE)) {
            return BibleBook.values()[volNum - 1].getNameWithSpaces() + " " + pageNum + ":" + sectionNum;
        } else if (author.equals(Author.HYMNS)) {
            return HymnBook.values()[volNum - 1].getName() + " " + pageNum + ":" + sentenceNum;
        }
        return "Can't get short readable reference";
    }

    /**
     * Get a path to the reference (relative to the Search Results page)
     * eg ../../target/fer/fer4.html
     *
     * @return path to reference
     */
    public String getPath() {
        switch (author) {
            case BIBLE:
                return LinkHelper.getHtmlLink(author, volNum, pageNum, sectionNum);
            default:
                return LinkHelper.getHtmlLink(author, volNum, pageNum, sentenceNum - 1);
        }
    }

}
