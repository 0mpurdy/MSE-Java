package mse.data;

import mse.helpers.HtmlHelper;

/**
 * Created by michaelpurdy on 30/12/2015.
 */
public class Result {

    Author author;
    Reference reference;

    String[] searchWords;

    String text;

    String resultBlock;

    // region constructors

    public Result(Author author, Reference reference, String text, String[] searchWords) {
        this.author = author;
        this.reference = reference;
        this.searchWords = searchWords;
        this.text = text;

        switch (author) {
            case HYMNS:
                this.resultBlock = getHymnsResultBlock();
        }
    }

    // region blockConstructors

    public Result(Author author, String resultBlock, String[] searchWords) {
        // this extracts a result from a result block
        this.author = author;
        this.searchWords = searchWords;

        switch (author) {
            case BIBLE:
                constructBibleFromBlock(resultBlock);
                break;
            case HYMNS:
                constructHymnFromBlock(resultBlock);
                break;
            default:
                constructMinistryFromBlock(resultBlock);
        }
    }

    private void constructBibleFromBlock(String resultBlock) {
        String[] lines = resultBlock.split("\\n");

        // get volume and page number from first line
        String link = HtmlHelper.extractFirstLink(lines[0]);

        int i = 0;
        while ((i < lines.length) && !lines[i].contains("mse-half")) i++;
        text = HtmlHelper.removeTabs(HtmlHelper.removeHtml(lines[i]));
        text += "\n" + HtmlHelper.removeTabs(HtmlHelper.removeHtml(lines[i + 1]));

        String bookName = link.substring(link.lastIndexOf("/") + 1, link.indexOf(".htm"));
        String chapter = link.substring(link.lastIndexOf("#") + 1, link.lastIndexOf(":"));
        String verse = link.substring(link.lastIndexOf(":") + 1);

        int volNum = BibleBook.getIndexFromString(bookName) + 1;
        int pageNum = Integer.parseInt(chapter);
        int verseNum = Integer.parseInt(verse);

        reference = new Reference(author, volNum, pageNum, verseNum);
    }

    private void constructHymnFromBlock(String resultBlock) {
        String[] lines = resultBlock.split("\\n");

        String link = HtmlHelper.extractFirstLink(lines[1]);
        text = lines[2].replace("<br>", "\n");
        text = HtmlHelper.removeTabs(HtmlHelper.removeHtml(text));

        String bookName = link.substring(link.lastIndexOf("/") + 1, link.indexOf(".htm"));
        String hymnNumber = link.substring(link.lastIndexOf("#") + 1);

        int volNum = HymnBook.getIndexFromString(bookName) + 1;
        int pageNum = Integer.parseInt(hymnNumber);

        reference = new Reference(author, volNum, pageNum, 0);
    }

    private void constructMinistryFromBlock(String resultBlock) {
        String[] lines = resultBlock.split("\\n");

        String link = HtmlHelper.extractFirstLink(lines[1]);
        text = HtmlHelper.removeTabs(HtmlHelper.removeHtml(lines[2]));

        String bookName = link.substring(link.lastIndexOf("/") + 1, link.indexOf(".htm"));
        String pageNumberString = link.substring(link.lastIndexOf("#") + 1);

        int volNum = getMinistryBookNumber(bookName);
        int pageNum = Integer.parseInt(pageNumberString);

        reference = new Reference(author, volNum, pageNum, 0);
    }

    private int getMinistryBookNumber(String bookName) {
        StringBuilder sb = new StringBuilder(bookName);
        int charPos = 0;
        while (charPos < sb.length() && !(Character.isDigit(sb.charAt(charPos)))) charPos++;
        return Integer.parseInt(sb.replace(0, charPos, "").toString());
    }

    // endregion

    public void print() {
        System.out.println(author.getCode() + ": " + reference.getReadableReference());
        System.out.println(text);
    }

    public void printBlock() {
        System.out.println(resultBlock);
    }

    public String getResultBlock() {
        switch (author) {
            case HYMNS:
                return getHymnsResultBlock();
            default:
                return getMinistryResultBlock();
        }
    }

    public String getHymnsResultBlock() {
        String markedLine = HtmlHelper.markLine(author, new StringBuilder(text), searchWords, "mse-mark");
        return "\t\t\t<div class=\"container padded\">\n" +
                "\t\t\t\t<a class=\"btn btn-primary\" href=\"" + reference.getPath() + "\" role=\"button\">" +
                reference.getReadableReference() + "</a>\n" +
                "\t\t\t\t<div class=\"spaced\">" + markedLine + "</div>\n" +
                "\t\t\t</div>";
    }

    public String getMinistryResultBlock() {
        String markedLine = HtmlHelper.markLine(author, new StringBuilder(text), searchWords, "mse-mark");
        return "\t\t\t<div class=\"container\">\n" +
                "\t\t\t\t<a class=\"btn\" href=\"" + reference.getPath() + "\">" +
                reference.getReadableReference() + "</a>\n" +
                "\t\t\t\t<div class=\"spaced\">" + markedLine + "</div>\n" +
                "\t\t\t</div>";
    }
}
