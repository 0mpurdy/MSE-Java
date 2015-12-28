package mse.helpers;

import mse.data.Author;
import mse.data.HymnBook;
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
        header.add("\t<div class=\"container centered\">");
        header.add("\t\t<p><img src=\"../../img/results.gif\"></p>");

        return header;
    }

    public static ArrayList<String> getAuthorResultsHeader(Author author, String searchWords) {
        // print the title of the author search results and search words

        String temp;
        if (author.isMinistry() || author == Author.BIBLE) {
            temp = " left-aligned";
        } else {
            temp = " centered";
        }
        final String extraClass = temp;

        return new ArrayList<String>() {{
            add("\t\t<hr>\n\t\t<h1>Results of search through " + author.getName() + "</h1>");
            add("\t\t<p>Searched: " + searchWords + "</p>");
            add("\t\t<div class=\"container" + extraClass + "\">");
        }};
    }

    public static String closeAuthorResultsBlock() {
        return "\t\t</div>";
    }

    public static ArrayList<String> getMinistryResultBlock(String path, String readableReference, String markedLine) {
        return new ArrayList<String>() {{
                add("\t\t\t<div class=\"container\">");
                add("\t\t\t\t<a class=\"btn\" href=\"..\\..\\" + path + "\"> "
                        + readableReference + "</a> ");
                add("\t\t\t\t<div class=\"spaced\">" + markedLine + "</div>");
                add("\t\t\t</div>");
            }};
    }

    public static void writeBibleResultBlock(ArrayList<String> resultText, AuthorSearchCache asc, String markedLine) {
        if (!asc.isWrittenBibleSearchTableHeader()) {
            resultText.add("\t\t\t<p><a class=\"btn\" href=\"..\\..\\" + asc.author.getTargetPath(asc.getVolumeName() + "#" + asc.pageNum + ":" + asc.getVerseNum()) + "\"> "
                    + asc.getReadableReference() + "</a></p>");
            resultText.add("\t\t\t<table class=\"bible-searchResult\">");
            resultText.add("\t\t\t\t<tr>");
            asc.setWrittenBibleSearchTableHeader(true);
            if (!asc.isSearchingDarby()) {
                resultText.add("\t\t\t\t\t<td class=\"mse-half\">" + asc.previousDarbyLine + "</td>");
            }
        }

        if (asc.isSearchingDarby()) {

            resultText.add("\t\t\t\t\t<td class=\"mse-half\">" + markedLine + "</td>");

        } else {

            resultText.add("\t\t\t\t\t<td class=\"mse-half\">" + markedLine + "</td>");

        }
    }

    public static ArrayList<String> finishSearchingSingleBibleScope(String line, ArrayList<String> resultText, AuthorSearchCache asc, boolean foundToken) {

        // if already written the header (found a result) and searching kjv
        if (asc.writtenBibleSearchTableHeader && !asc.searchingDarby) {

            if (!foundToken) {
                writeBibleResultBlock(resultText, asc, removeHtml(line));
            }

            resultText.add("\t\t\t\t</tr>");
            resultText.add("\t\t\t</table>");

            asc.writtenBibleSearchTableHeader = false;

            // if searching jnd and not yet found a result
        } else if (asc.searchingDarby && !asc.writtenBibleSearchTableHeader) asc.previousDarbyLine = line;

        asc.searchingDarby = !asc.searchingDarby;

        return resultText;
    }

    public static ArrayList<String> getHymnsResultBlock(String path, String readableReference, String markedLine) {
        return new ArrayList<String>() {{
                add("\t\t\t<div class=\"container padded\">");
                add("\t\t\t\t<a class=\"btn btn-primary\" href=\"..\\..\\" + path + "\" role=\"button\"> "
                        + readableReference + "</a>");
                add("\t\t\t\t<div class=\"spaced\">" + markedLine + "</div>");
                add("\t\t\t</div>");
            }};
    }

    public static String getSingleAuthorResults(String name, int numResults) {
        return "\t\t<div class=\"spaced\">Number of results for " + name + ": " + numResults + "</div>";
    }

    public static String getHtmlFooter(String end) {
        return end + "\n</body>\n\n</html>";
    }

    public static String removeHtml(String line) {
        return removeHtml(new StringBuilder(line)).toString();
    }

    public static StringBuilder removeHtml(StringBuilder line) {
        int charPos = 0;

        while (++charPos < line.length()) {
            if (line.charAt(charPos) == '<') {
                int tempCharIndex = charPos + 1;
                while (tempCharIndex < line.length() - 1 && line.charAt(tempCharIndex) != '>') tempCharIndex++;
                tempCharIndex++;
                line.replace(charPos, tempCharIndex, "");
            }
        }

        return line;
    }

    public static void writeHymnbookName(ArrayList<String> resultText, AuthorSearchCache asc) {
        resultText.add(String.format("\t\t\t<p class=\"%s\"><a href=\"%s\">%s</a></p>",
                "results-hymnbook-name",
                "..\\..\\" + asc.author.getTargetPath(asc.getVolumeName()),
                HymnBook.values()[asc.volNum - 1].getName()));
    }
}
