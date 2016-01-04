package mse.data;

import mse.helpers.HtmlHelper;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by Michael Purdy on 04/01/2016.
 */
public class AuthorResults {

    private Author author;
    private String searchWords;
    private ArrayList<Result> results;

    public AuthorResults(Author author, String searchWords, ArrayList<Result> results) {
        this.author = author;
        this.searchWords = searchWords;
        this.results = results;
    }

    public void writeAllResults(PrintWriter pw) {

        pw.println(HtmlHelper.getAuthorResultsHeader(author, searchWords));

        for (Result result : results) {
            pw.println(result.getBlock());
        }

        HtmlHelper.closeAuthorContainer(pw);

        pw.println(HtmlHelper.getSingleAuthorResults(author.getName(), results.size()));

    }

    public int getNumResults() {
        return results.size();
    }
}
