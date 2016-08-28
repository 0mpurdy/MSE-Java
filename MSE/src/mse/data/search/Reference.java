package mse.data.search;

import mse.data.author.BibleBook;
import mse.data.author.HymnBook;
import mse.data.author.Author;

/**
 * A reference to a search result
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

    public String getFileName() {
        if (author.isMinistry()) {
            return author.getTargetVolumeName(volNum);
        } else if (author.equals(Author.BIBLE)) {
            return BibleBook.values()[volNum - 1].getTargetFilename();
        } else if (author.equals(Author.HYMNS)) {
            return HymnBook.values()[volNum - 1].getTargetFilename();
        } else {
            return "";
        }
    }

    public String getPath() {
        switch (author) {
            case BIBLE:
                return author.getRelativeHtmlTargetPath(getFileName() + "#" + pageNum + ":" + sectionNum);
            default:
                if ((sentenceNum - 1) > 0) {
                    return author.getRelativeHtmlTargetPath(getFileName()) + "#" + pageNum + ":" + (sentenceNum - 1);
                } else {
                    return author.getRelativeHtmlTargetPath(getFileName()) + "#" + pageNum;
                }
        }
    }

}
