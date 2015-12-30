package mse.data;

import mse.search.AuthorSearchCache;

/**
 * Created by michaelpurdy on 30/12/2015.
 */
public class Reference {

    private Author author;

    // public allows faster access
    public int volNum, pageNum, verseNum;

    public Reference(Author author, Reference reference) {
        this.author = author;
        this.volNum = reference.volNum;
        this.pageNum = reference.pageNum;
        this.verseNum = reference.verseNum;
    }

    public Reference(Author author, int volNum, int pageNum, int verseNum) {
        this.author = author;
        this.volNum = volNum;
        this.pageNum = pageNum;
        this.verseNum = verseNum;
    }

    public String getReadableReference() {
        switch (author) {
            case BIBLE:
                return BibleBook.values()[volNum - 1].getName() + " chapter " + pageNum + ":" + verseNum;
            case HYMNS:
                return Integer.toString(pageNum);
            default:
                return author.getCode() + " volume " + volNum + " page " + pageNum;
        }
    }

    public String getShortReadableReference() {
        if (author.isMinistry()) {
            return author.getCode() + "vol " + volNum + ":" + pageNum;
        } else if (author.equals(Author.BIBLE)) {
            return BibleBook.values()[volNum - 1].getName() + " " + pageNum + ":" + verseNum;
        } else if (author.equals(Author.HYMNS)) {
            return HymnBook.values()[volNum - 1].getName() + " " + pageNum + ":" + verseNum;
        }
        return "Can't get short readable reference";
    }

    public String getFileName() {
        if (author.isMinistry()) {
            return author.getCode() + volNum + ".htm";
        } else if (author.equals(Author.BIBLE)) {
            return BibleBook.values()[volNum - 1].getName() + ".htm";
        } else if (author.equals(Author.HYMNS)) {
            return HymnBook.values()[volNum - 1].getOutputFilename();
        } else {
            return "";
        }
    }

    public String getPath() {
        switch (author) {
            case BIBLE:
                return "../../" + author.getTargetPath(getFileName() + "#" + pageNum + ":" + verseNum);
            default:
                return "../../" + author.getTargetPath(getFileName()) + "#" + pageNum;
        }
    }
}
