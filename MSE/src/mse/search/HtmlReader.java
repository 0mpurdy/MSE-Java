package mse.search;

import mse.common.LogLevel;
import mse.common.LogRow;
import mse.data.Author;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by mj_pu_000 on 23/12/2015.
 */
public class HtmlReader {

    private BufferedReader br;
    private ArrayList<LogRow> searchLog;

    public HtmlReader(String path, ArrayList<LogRow> searchLog) {
        try {
            this.br = new BufferedReader(new FileReader(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.searchLog = searchLog;
    }

    public int findNextPage(AuthorSearchCache asc) {
        // set the prevLine in asc to the last line of the previous page
        // return the page number of the next page
        // asc.Line is the page number and br is at the line of the current page number
        try {
            if (asc.author.equals(Author.BIBLE)) {
                return findNextBiblePage(asc, br);
            } else if (asc.author.equals(Author.HYMNS)) {
                return findNextHymnPage(asc, br);
            } else {
                return findNextMinistryPage(asc, br);
            }
        } catch (IOException e) {
            log(LogLevel.HIGH, e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    private int findNextHymnPage(AuthorSearchCache asc, BufferedReader br) throws IOException {

        int cPageNum = 0;
        boolean foundPage = false;

        // clear the previous line
        asc.prevLine = "";

        // read until page number = page ref
        // or if page number is page before next reference get the last line
        while (!foundPage) {

            if (asc.line != null) {

                if (asc.line.contains("<td class=\"hymn-number\">")) {
                    asc.line = br.readLine();

                    try {

                        // get the current page's number
                        cPageNum = getPageNumber(asc.line, "=", ">");

                        if (asc.pageNum == cPageNum) {
                            return cPageNum;
                        }

                    } catch (NumberFormatException nfe) {
                        log(LogLevel.HIGH, "Error formatting page number in search: " + asc.author.getCode() + " " + asc.volNum + ":" + cPageNum);
                        return 0;
                    }
                }
            } else {
                log(LogLevel.HIGH, "NULL line when reading " + asc.getShortReadableReference());
                break;
            }
            asc.line = br.readLine();

        } // found start of page

        // shouldn't reach here
        return 0;
    }

    private int findNextBiblePage(AuthorSearchCache asc, BufferedReader br) throws IOException {

        int cPageNum = 0;
        boolean foundPage = false;

        // clear the previous line
        asc.prevLine = "";

        // read until page number = page ref
        // or if page number is page before next reference get the last line
        while (!foundPage) {

            if (asc.line != null) {

                if (asc.line.contains("<td colspan=\"3\" class=\"chapterTitle\">")) {

                    try {

                        // get the current page's number
                        int startIndex = asc.line.indexOf("name=") + 5;
                        int endIndex = asc.line.indexOf('>', startIndex);
                        String cPageNumStr = asc.line.substring(startIndex, endIndex);
                        cPageNum = Integer.parseInt(cPageNumStr);

                        // if it is the previous page
                        if (asc.pageNum == cPageNum + 1) {
                            asc.prevLine = getLastLineOfBiblePage(br);

                            // skip close and open of table and open of row
                            br.readLine();
                            br.readLine();
                            br.readLine();

                            // set found page get page number
                            asc.line = br.readLine();

                            startIndex = asc.line.indexOf("name=") + 5;
                            endIndex = asc.line.indexOf('>', startIndex);
                            cPageNumStr = asc.line.substring(startIndex, endIndex);
                            cPageNum = Integer.parseInt(cPageNumStr);

                            if (asc.pageNum == cPageNum) {
                                return cPageNum;
                            } else {
                                // error next page not after previous page
                                log(LogLevel.LOW, "Couldn't find search page: " + asc.author.getCode() + " " + asc.volNum + ":" + asc.pageNum);
                                return 0;
                            }
                        } else if (asc.pageNum == cPageNum) {
                            return cPageNum;
                        }
                    } catch (NumberFormatException nfe) {
                        log(LogLevel.HIGH, "Error formatting page number in search: " + asc.author.getCode() + " " + asc.volNum + ":" + cPageNum);
                        return 0;
                    }
                }
            } else {
                log(LogLevel.HIGH, "NULL line when reading " + asc.getShortReadableReference());
                break;
            }
            asc.line = br.readLine();

        } // found start of page

        // shouldn't reach here
        return 0;
    }

    private int findNextMinistryPage(AuthorSearchCache asc, BufferedReader br) throws IOException {

        int cPageNum = 0;
        boolean foundPage = false;

        // clear the previous line
        asc.prevLine = "";

        // read until page number = page ref
        // or if page number is page before next reference get the last line
        while (!foundPage) {

            if (asc.line != null) {

                if (asc.line.contains("class=\"page-number\"")) {
                    asc.line = br.readLine();

                    try {

                        // get the current page's number
                        cPageNum = Integer.parseInt(asc.line.substring(asc.line.indexOf("=") + 1, asc.line.indexOf('>')));

                        // if it is the previous page
                        if (asc.pageNum == cPageNum + 1) {
                            asc.prevLine = getLastLineOfPage(br);

                            // set found page get page number
                            asc.line = br.readLine();

                            // skip any footnotes
                            while (asc.line.contains("class=\"footnote\"")) {
                                br.readLine();
                                br.readLine();
                                asc.line = br.readLine();
                            }

                            cPageNum = Integer.parseInt(asc.line.substring(asc.line.indexOf("=") + 1, asc.line.indexOf('>')));

                            if (asc.pageNum == cPageNum) {
                                return cPageNum;
                            } else {
                                // error next page not after previous page
                                log(LogLevel.LOW, "Couldn't find search page: " + asc.author.getCode() + " " + asc.volNum + ":" + asc.pageNum);
                                return 0;
                            }
                        } else if (asc.pageNum == cPageNum) {
                            return cPageNum;
                        }
                    } catch (NumberFormatException nfe) {
                        log(LogLevel.HIGH, "Error formatting page number in search: " + asc.author.getCode() + " " + asc.volNum + ":" + cPageNum);
                        return 0;
                    }
                }
            } else {
                log(LogLevel.HIGH, "NULL line when reading " + asc.author.getCode() + " vol " + asc.volNum + " page " + asc.pageNum);
                break;
            }
            asc.line = br.readLine();

        } // found start of page

        // shouldn't reach here
        return 0;
    }

    private int getPageNumber(String line, String splitStart, String splitEnd) {
        int start = line.indexOf(splitStart) + splitStart.length();
        int end = line.indexOf(splitEnd, start);
        return Integer.parseInt(line.substring(start, end));
    }

    private String getLastLineOfBiblePage(BufferedReader br) throws IOException {
        // returns the last darby line of the bible and moves
        // the buffered reader pointer to point at the end of the chapter
        // (empty line before </table>)

        // skip synopsis
        for (int i = 0; i < 6; i++) br.readLine();

        // while still on the page get the darby line
        String darbyLine = "";
        while ((br.readLine()).contains("<tr")) {
            // skip verse number
            br.readLine();

            // set the line
            darbyLine = br.readLine();

            // skip kjv and end of row tag
            br.readLine();
            br.readLine();
        }

        return darbyLine;
    }

    private String getLastLineOfPage(BufferedReader br) throws IOException {

        // for each paragraph in the page
        // skip a line and check if the next line is a heading or paragraph
        br.readLine();

        String tempLine = br.readLine();
        String lastLine = "";

        // while not the last line
        while (tempLine.contains("class=\"heading\"") || tempLine.contains("class=\"paragraph\"") || tempLine.contains("class=\"footnote\"")) {

            // read the line of text
            lastLine = br.readLine();

            // skip the </div> tag line
            br.readLine();

            // read the next line class
            tempLine = br.readLine();
        }

        return lastLine;

    }

    public String getNextSectionHeader(AuthorSearchCache asc) {
        // returns the line that contains the class of the next section

        try {

            if (asc.author.isMinistry()) {
                // skip a line
                br.readLine();
                return br.readLine();
            } else if (asc.author.equals(Author.BIBLE)) {

                // if still in same section do not change section header
                if (!asc.isSearchingDarby()) return asc.currentSectionHeader;

                // skip a line
                br.readLine();

                return br.readLine();

            } else if (asc.author.equals(Author.HYMNS)) {

                String tempLine = br.readLine();

                while (!tempLine.contains("class") && !tempLine.contains("</body>")) {
                    tempLine = br.readLine();
                }

                return tempLine;

            }
        } catch (IOException e) {
            log(LogLevel.HIGH, e.getMessage());
            e.printStackTrace();
        }
        return null;

    }

    public String getNextSection(AuthorSearchCache asc) {

        try {

            if (asc.author.isMinistry()) {
                return br.readLine();
            } else if (asc.author.equals(Author.BIBLE)) {
                String line = "";
                if (asc.isSearchingDarby()) {

                    // skip verse number
                    br.readLine();

                    // read jnd
                    line = br.readLine();

                } else {
                    line = br.readLine();
                }
                return line;
            } else if (asc.author.equals(Author.HYMNS)) {

                // skip verse anchor tag
                for (int i = 0; i < 3; i++) br.readLine();

                String output = "";
                String temp = br.readLine();

                while (!temp.contains("</td>")) {
                    output += temp;
                    temp = br.readLine();
                }

                return output;

            }
        } catch (IOException e) {
            log(LogLevel.HIGH, e.getMessage());
            e.printStackTrace();
        }
        return null;
    }


    public String getFirstSectionHeader(AuthorSearchCache asc)  {
        try {
            if (asc.author.isMinistry()) {
                // skip a line
                br.readLine();
                return br.readLine();
            } else if (asc.author.equals(Author.BIBLE)) {

                // skip synopsis
                for (int i = 0; i < 6; i++) br.readLine();

                return br.readLine();

            } else if (asc.author.equals(Author.HYMNS)) {

                // skip author and metre
//            String temp = "";
                for (int i = 0; i < 7; i++) /*temp =*/ br.readLine();

                return br.readLine();

            }
        } catch (IOException e) {
            log(LogLevel.HIGH, e.getMessage());
            e.printStackTrace();
        }
        return null;

    }

    private void log(LogLevel logLevel, String message) {
        searchLog.add(new LogRow(logLevel,message));
    }

    public void close() {
        if (br != null) try {
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String readContentLine() {
        String contentLine = null;
        try {
            contentLine = br.readLine();
        } catch (IOException e) {
            log(LogLevel.HIGH, e.getMessage());
            e.printStackTrace();
        }
        return contentLine;
    }
}
