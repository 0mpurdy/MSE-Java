package mse.helpers;

import mse.data.author.Author;
import mse.data.author.HymnBook;
import mse.search.AuthorSearchCache;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * @author Michael Purdy
 *      Helps with creation of HTML files
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

    public static String getResultsHeader(String mseStyleLocation) {
        String header = getHtmlHeader("Results", mseStyleLocation);
        header += "\n<body>" +
                "\n\t<div class=\"container centered\">" +
                "\n\t\t<p><img src=\"../../img/results.gif\"></p>";

        return header;
    }

    /**
     * Print the title of the author and the words that were searched within that author
     * @param author Name of the author
     * @param searchWords Words that were searched
     * @return String HTML header for the author results section
     */
    public static String getAuthorResultsHeader(Author author, String searchWords) {
        // print the title of the author search results and search words
        return "\t\t<hr>\n\t\t<h1>Results of search through " + author.getName() + "</h1>\n" +
                "\t\t<p>Searched: " + searchWords + "</p>\n";
    }

    /**
     * Get the opening of the container that contains the author's results
     * @param author Author that the results pertain to
     * @return String HTML opening tag for the author results container
     */
    public static String getAuthorResultsContainerOpen(Author author) {
        final String extraClass = (author.isMinistry() || author == Author.BIBLE) ? " left-aligned" : " centered";
        return "\t\t<div class=\"container" + extraClass + "\">";
    }

    public static String getFormattedHymnbookLink(AuthorSearchCache asc) {
        return String.format("\t\t\t<p class=\"%s\"><a href=\"%s\">%s</a></p>",
                "results-hymnbook-name",
                asc.reference.getPath(),
                HymnBook.values()[asc.reference.volNum - 1].getName());
    }

    public static void closeAuthorContainer(PrintWriter pw) {
        pw.println("\t\t</div>");
    }

    /**
     * Highlight all the search words in the line with an html <mark/> tag
     *
     * @return The result in HTML format with search words highlighted
     */
    public static String markLine(Author author, StringBuilder line, String[] words, String emphasis) {

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
                if (startOfWord > 0 && Character.isLetter(line.charAt(startOfWord - 1))) continue;
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
        return "\t\t<div class=\"spaced\">Number of results: " + numResults + "</div>";
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

    // region tokenHelp

    /**
     * Remove the HTML and split the line into tokens by non-word characters
     * @param line The line to tokenize
     * @return String[] tokens
     */
    public static String[] tokenizeLine(String line) {
        line = removeHtml(line);

        // split the line into tokens (words) by non-word characters
        return tokenizeArray(line.split("[\\W]"));
    }

    public static String[] tokenizeArray(String[] tokens) {

        ArrayList<String> newTokens = new ArrayList<>();

        // make each token into a word that can be searched
        for (String token : tokens) {
            token = token.toUpperCase();
            if (!isAlpha(token)) {
                token = processString(token);
            }
            newTokens.add(token);
        } // end for each token

        String[] newTokensArray = new String[newTokens.size()];
        newTokensArray = newTokens.toArray(newTokensArray);

        return newTokensArray;
    }


    public static boolean isAlpha(String token) {
        char[] chars = token.toCharArray();

        for (char c : chars) {
            if (!Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }

    public static String processString(String token) {
        for (char c : token.toCharArray()) {
            if (!Character.isLetter(c)) {
                token = token.replace(Character.toString(c), "");
            }
        }

        return token;
    }

    // endregion

    // region printableArrays

    public static String printableArray(String[] array) {
        StringBuilder printableArray = new StringBuilder();

        for (String word : array) {
            printableArray.append(word).append(", ");
        }

        if (printableArray.length() < 2) return "";

        // remove last comma
        return printableArray.toString().substring(0, printableArray.length() - 2);
    }

    // endregion

}
