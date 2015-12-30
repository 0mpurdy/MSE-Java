package mse.helpers;

import mse.data.Author;
import mse.data.HymnBook;
import mse.data.Reference;
import mse.search.AuthorSearchCache;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by Michael Purdy on 02/12/2015.
 * <p>
 * Helps with creation of HTML files
 */
public class HtmlHelper {

    private static String bootstrapLocation = "../../bootstrap/css/bootstrap.css";

    // region genericStart

    public static void writeHtmlHeader(PrintWriter pw, String title, String mseStyleLocation) {
        pw.println(getHtmlHeader(title, mseStyleLocation));
    }

    public static String getHtmlHeader(String title, String mseStyleLocation) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n\n" +
                "<head>\n" +
                "\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
                "\t<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
                "\t<title>" + title + "</title>\n" +
                "\t<link rel=\"stylesheet\" type=\"text/css\" href=\"" + bootstrapLocation + "\">\n" +
                "\t<link rel=\"stylesheet\" type=\"text/css\" href=\"" + mseStyleLocation + "\">\n" +
                "</head>";
    }

    public static void writeStart(PrintWriter pw) {
        pw.println(getStart());
    }

    public static void writeStartAndContainer(PrintWriter pw) {
        pw.println(getStart() + "\n" + getStartContainer());
    }

    public static String getStart() {
        return "\n<body>";
    }

    public static String getStartContainer() {
        return "\t<div class=\"container\">";
    }

    // endregion

    // region resultsPage

    public static String getResultsHeader(String title, String mseStyleLocation) {
        String header = getHtmlHeader(title, mseStyleLocation);
        header += "\n<body>" +
                "\n\t<div class=\"container centered\">" +
                "\n\t\t<p><img src=\"../../img/results.gif\"></p>";

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

    public static void writeHymnbookName(ArrayList<String> resultText, AuthorSearchCache asc) {
        resultText.add(String.format("\t\t\t<p class=\"%s\"><a href=\"%s\">%s</a></p>",
                "results-hymnbook-name",
                "..\\..\\" + asc.author.getTargetPath(asc.reference.getFileName()),
                HymnBook.values()[asc.reference.volNum - 1].getName()));
    }

    public static String closeAuthorResultsBlock() {
        return "\t\t</div>";
    }

    public static void writeBibleResultBlock(ArrayList<String> resultText, AuthorSearchCache asc, String markedLine) {
        if (!asc.isWrittenBibleSearchTableHeader()) {
            resultText.add("\t\t\t<p><a class=\"btn\" href=\"..\\..\\" + asc.author.getTargetPath(asc.reference.getFileName() + "#" + asc.reference.pageNum + ":" + asc.reference.verseNum) + "\"> "
                    + asc.reference.getReadableReference() + "</a></p>");
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

    public static String markLine(Author author, StringBuilder line, String[] words, String emphasis) {
        // highlight all the search words in the line with an html <mark/> tag

        // remove any html already in the line
        int charPos = 0;

        if (!author.equals(Author.HYMNS)) line = HtmlHelper.removeHtml(line);

        for (String word : words) {

            charPos = 0;

            // while there are still more words matching the current word in the line
            // and the char position hasn't exceeded the line
            int startOfWord;
            int endOfWord;
            while (charPos < line.length() && ((startOfWord = line.toString().toLowerCase().indexOf(word.toLowerCase(), charPos)) != -1)) {

                endOfWord = startOfWord + word.length();
                charPos = endOfWord;

                // if the word has a letter before it or after it then skip it
                if (startOfWord >= 0 && Character.isLetter(line.charAt(startOfWord - 1))) continue;
                if (endOfWord < line.length() && Character.isLetter(line.charAt(endOfWord))) continue;

                // otherwise mark the word
                String currentCapitalisation = line.substring(startOfWord, endOfWord);
                String openDiv = "<span class=\"" + emphasis + "\">";
                String closeDiv = "</span>";
                line.replace(startOfWord, endOfWord, openDiv + currentCapitalisation + closeDiv);

                // set the char position to after the word
                charPos = endOfWord + openDiv.length() + closeDiv.length();
            }
        }

        return line.toString();
    }

    public static String getSingleAuthorResults(String name, int numResults) {
        return "\t\t<div class=\"spaced\">Number of results for " + name + ": " + numResults + "</div>";
    }

    // endregion

    public static String getHtmlFooter(String end) {
        return end + "\n</body>\n\n</html>";
    }

    // region htmlManipulation

    public static String removeHtml(String line) {
        return removeHtml(new StringBuilder(line)).toString();
    }

    public static StringBuilder removeHtml(StringBuilder line) {
        int charPos = 0;

        while (charPos < line.length()) {
            if (line.charAt(charPos) == '<') {
                int tempCharIndex = charPos + 1;
                while (tempCharIndex < line.length() - 1 && line.charAt(tempCharIndex) != '>') tempCharIndex++;
                tempCharIndex++;
                line.replace(charPos, tempCharIndex, "");
            } else {
                charPos++;
            }
        }

        return line;
    }

    public static String removeTabs(String line) {
        return removeTabs(new StringBuilder(line)).toString();
    }

    public static StringBuilder removeTabs(StringBuilder line) {
        int charPos = 0;

        while (charPos < line.length()) {
            if (line.charAt(charPos) == '\t') {
                line.replace(charPos, charPos + 1, "");
            } else {
                charPos++;
            }
        }

        return line;
    }

    public static String extractSubstring(String line, String pre, String sub) {
        return line.substring(line.indexOf(pre) + pre.length(), line.lastIndexOf(sub));
    }

    public static String extractFirstLink(String line) {
        String linkStart = "href=\"";
        String link = line.substring(line.indexOf(linkStart) + linkStart.length());
        return link.substring(0, link.indexOf("\""));
    }

    // endregion
}
