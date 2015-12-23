package mse.helpers;

import mse.data.Author;
import mse.search.AuthorSearchCache;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by Michael on 02/12/2015.
 */
public class HtmlHelper {

    private static String bootstrapLocation = "../../bootstrap/css/bootstrap.css";

    public static void writeHtmlHeader(PrintWriter pw, String title, String mseStyleLocation) {

        pw.println("<!DOCTYPE html>\n" +
                "\n" +
                "<html>" +
                "\n" +
                "<head>\n" +
                "\t<title>" + title + "</title>\n" +
                "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + bootstrapLocation + "\">\n" +
                "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + mseStyleLocation + "\">\n" +
                "</head>");
    }

    public static ArrayList<String> getHtmlHeaderLines(String title, String mseStyleLocation) {
        return new ArrayList<String>() {{
            add("<!DOCTYPE html>");
            add("");
            add("<html>");
            add("");
            add("<head>");
            add("\t<title>" + title + "</title>");
            add("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + bootstrapLocation + "\">");
            add("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + mseStyleLocation + "\">");
            add("</head>");
        }};
    }

    public static ArrayList<String> getResultsHeaderLines(String title, String mseStyleLocation) {
        ArrayList header = new ArrayList();
        header.addAll(getHtmlHeaderLines(title, mseStyleLocation));
        header.add("<body>");
        header.add("<div class=\"container centered\">");
        header.add("\t<p><img src=\"../../img/results.gif\"></p>");

        return header;
    }

    public static ArrayList<String> getAuthorResultsHeader(Author author, String searchWords) {
        // print the title of the author search results and search words

        String temp = "";
        if (author.isMinistry() || author == Author.BIBLE) {
            temp = " left-aligned";
        } else {
            temp = " centered";
        }
        final String extraClass = temp;

        return new ArrayList<String>() {{
            add("\t<hr>\n\t<h1>Results of search through " + author.getName() + "</h1>");
            add("\t<p>\n\t\tSearched: " + searchWords + "\n\t</p>");
            add("\t<div class=\"container" + extraClass + "\">");
        }};
    }

    public static String closeAuthorResultsBlock() {
        return "</div>";
    }

    public static ArrayList<String> getMinistryResultBlock(String path, String readableReference, String markedLine) {
        return new ArrayList<String>() {{
                add("\t<div class=\"container\">");
                add("\t\t<a class=\"btn\" href=\"..\\..\\" + path + "\"> "
                        + readableReference + "</a> ");
                add("\t\t<div class=\"spaced\">" + markedLine + "</div>");
                add("\t</div>");
            }};
    }

    public static void writeBibleResultBlock(ArrayList<String> resultText, AuthorSearchCache asc, String volumeName, String readableReference, String markedLine) {
        if (!asc.isWrittenBibleSearchTableHeader()) {
            resultText.add("\t<p><a class=\"btn\" href=\"..\\..\\" + asc.author.getTargetPath(volumeName + "#" + asc.pageNum + ":" + asc.getVerseNum()) + "\"> "
                    + readableReference + "</a></p>");
            resultText.add("\t<table class=\"bible-searchResult\">");
            resultText.add("\t\t<tr>");
            asc.setWrittenBibleSearchTableHeader(true);
            if (!asc.isSearchingDarby()) {
                resultText.add("\t\t\t<td class=\"mse-half\">" + asc.previousDarbyLine + "</td>");
            }
        }

        if (asc.isSearchingDarby()) {

            resultText.add("\t\t\t<td class=\"mse-half\">" + markedLine + "</td>");

        } else {

            resultText.add("\t\t\t<td class=\"mse-half\">" + markedLine + "</td>");

        }
    }

    public static ArrayList<String> getHymnsResultBlock(String path, String readableReference, String markedLine) {
        return new ArrayList<String>() {{
                add("\t<div class=\"container padded\">");
                add("\t\t<a class=\"btn btn-primary\" href=\"..\\..\\" + path + "\" role=\"button\"> "
                        + readableReference + "</a>");
                add("<div class=\"spaced\">" + markedLine + "</div>");
                add("\t</div>");
            }};
    }

    public static String getSingleAuthorResults(String name, int numResults) {
        return "<div class=\"spaced\">Number of results for " + name + ": " + numResults + "</div>";
    }

    public static String getHtmlFooter(String end) {
        return end + "\n</body>\n\n</html>";
    }
}
