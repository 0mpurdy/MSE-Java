package mse.data.search;

import mse.data.author.BibleBook;
import mse.data.author.HymnBook;
import mse.data.author.Author;
import mse.helpers.HtmlHelper;

import java.util.regex.Pattern;

/**
 * Created by michaelpurdy on 30/12/2015.
 */
public class Result implements IResult {

    Author author;
    Reference reference;

    String[] searchWords;

    String text;

    private boolean newHymnBook;
    private String hymnBookLink;

    // region constructors

    public Result(Author author, Reference reference, String text, String[] searchWords) {
        this.author = author;
        this.reference = reference;
        this.searchWords = searchWords;
        this.text = text;
        this.newHymnBook = false;
    }

    public Result(Author author, Reference reference, String text, String[] searchWords, String hymnBookLink) {
        this.author = author;
        this.reference = reference;
        this.searchWords = searchWords;
        this.text = text;
        this.newHymnBook = true;
        this.hymnBookLink = hymnBookLink;
    }

    // endregion

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
        text += " <!-> " + HtmlHelper.removeTabs(HtmlHelper.removeHtml(lines[i + 1]));

        String bookName = link.substring(link.lastIndexOf("/") + 1, link.indexOf(".html"));
        String chapter = link.substring(link.lastIndexOf("#") + 1, link.lastIndexOf(":"));
        String verse = link.substring(link.lastIndexOf(":") + 1);

        int volNum = BibleBook.getIndexFromString(bookName) + 1;
        int pageNum = Integer.parseInt(chapter);
        int verseNum = Integer.parseInt(verse);

        reference = new Reference(author, volNum, pageNum, verseNum, 0);
    }

    private void constructHymnFromBlock(String resultBlock) {
        String[] lines = resultBlock.split("\\n");

        String link = HtmlHelper.extractFirstLink(lines[1]);
        text = lines[2].replace("<br>", "\n");
        text = HtmlHelper.removeTabs(HtmlHelper.removeHtml(text));

        String bookName = link.substring(link.lastIndexOf("/") + 1, link.indexOf(".html"));
        String hymnNumber = link.substring(link.lastIndexOf("#") + 1);

        int volNum = HymnBook.getIndexFromString(bookName) + 1;
        int pageNum = Integer.parseInt(hymnNumber);

        reference = new Reference(author, volNum, pageNum, 0, 0);
    }

    private void constructMinistryFromBlock(String resultBlock) {
        String[] lines = resultBlock.split("\\n");

        String link = HtmlHelper.extractFirstLink(lines[1]);
        text = HtmlHelper.removeTabs(HtmlHelper.removeHtml(lines[2]));

        String bookName = link.substring(link.lastIndexOf("/") + 1, link.indexOf(".html"));
        String pageNumberString = link.substring(link.lastIndexOf("#") + 1);

        int volNum = getMinistryBookNumber(bookName);
        int pageNum = Integer.parseInt(pageNumberString);

        reference = new Reference(author, volNum, pageNum, 0, 0);
    }

    private int getMinistryBookNumber(String bookName) {
        StringBuilder sb = new StringBuilder(bookName);
        int charPos = 0;
        while (charPos < sb.length() && !(Character.isDigit(sb.charAt(charPos)))) charPos++;
        return Integer.parseInt(sb.replace(0, charPos, "").toString());
    }

    // endregion

    // region refine

    public boolean refine(boolean contains, String[] refineWords) {

        String[] lineTokens = HtmlHelper.tokenizeLine(text);

        if (contains) {
            return SearchType.SENTENCE.search(lineTokens, refineWords);
        } else {
            return !SearchType.SENTENCE.search(lineTokens, refineWords);
        }

    }

    // endregion

    // region output

    /**
     * Print the result on the console (for debugging)
     */
    public void print() {
        System.out.println(author.getCode() + ": " + reference.getReadableReference());
        System.out.println(text);
    }

    /**
     * Print just the result block on the console (for debugging)
     */
    public void printBlock() {
        System.out.println(getBlock());
    }

    /**
     * Get the result block HTML
     *
     * @return The result in HTML format
     */
    @Override
    public String getBlock() {
        switch (author) {
            case BIBLE:
                return getBibleBlock();
            case HYMNS:
                return getHymnsBlock();
            default:
                return getMinistryBlock();
        }
    }

    /**
     * Get the result block HTML for a Hymn result
     *
     * @return The result in HTML format for a Hymn
     */
    public String getHymnsBlock() {
        String brokenText = text.replace("\n", "<br>");
        String block = "\t\t\t<div class=\"container padded\">\n" +
                "\t\t\t\t<a class=\"btn btn-primary\" href=\"" + reference.getPath() + "\" role=\"button\">" +
                reference.getReadableReference() + "</a>\n" +
                "\t\t\t\t<div class=\"spaced\">" + getMarkedLine(brokenText) + "</div>\n" +
                "\t\t\t</div>";
        if (newHymnBook) block = hymnBookLink + "\n" + block;
        return block;
    }


    /**
     * Get the result block HTML for a Ministry result
     *
     * @return The result in HTML format for a Ministry result
     */
    public String getMinistryBlock() {
        String markedLine = HtmlHelper.markLine(author, new StringBuilder(text), searchWords, "mse-mark");
        return "\t\t\t<div class=\"container\">\n" +
                "\t\t\t\t<a class=\"btn\" href=\"" + reference.getPath() + "\">" +
                reference.getReadableReference() + "</a>\n" +
                "\t\t\t\t<div class=\"spaced\">" + markedLine + "</div>\n" +
                "\t\t\t</div>";
    }

    /**
     * Get the result block HTML for a Bible result
     *
     * @return The result in HTML format for a Bible result
     */
    public String getBibleBlock() {

        String[] lines = text.split(Pattern.quote(" <!-> "));

        return "\t\t\t<p><a class=\"btn\" href=\"" + reference.getPath() + "\"> "
                + reference.getReadableReference() + "</a></p>\n" +
                "\t\t\t<table class=\"bible-searchResult\">\n" +
                "\t\t\t\t<tr>\n" +
                "\t\t\t\t\t<td class=\"mse-half\">" + getMarkedLine(lines[0]) + "</td>\n" +
                "\t\t\t\t\t<td class=\"mse-half\">" + getMarkedLine(lines[1]) + "</td>\n" +
                "\t\t\t\t</tr>\n" +
                "\t\t\t</table>";
    }

    /**
     * Highlight the search words in a line
     *
     * @return A line with the search results highlighted
     */
    private String getMarkedLine(String line) {
        return HtmlHelper.markLine(author, new StringBuilder(line), searchWords, "mse-mark");
    }

    // endregion

}
