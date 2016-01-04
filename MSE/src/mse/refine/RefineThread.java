package mse.refine;

import mse.common.Config;
import mse.common.ILogger;
import mse.common.LogLevel;
import mse.common.LogRow;
import mse.data.Author;
import mse.data.AuthorResults;
import mse.data.Result;
import mse.helpers.HtmlHelper;
import mse.helpers.HtmlReader;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by Michael Purdy on 28/12/2015.
 * <p>
 * Thread for performing the refine function
 */
public class RefineThread extends Thread {

    private Config cfg;
    private ILogger logger;
    private boolean contains;
    private String[] refineTokens;

    public RefineThread(Config cfg, ILogger logger, boolean contains, String[] refineTokens) {
        this.cfg = cfg;
        this.logger = logger;
        this.contains = contains;
        this.refineTokens = refineTokens;
    }


    @Override
    public void run() {

        if (refineTokens.length > 0) {
            refineResults(contains, refineTokens);
        }

        try {
            Desktop.getDesktop().open(new File(cfg.getResDir() + cfg.getResultsFile()));
        } catch (IOException | IllegalArgumentException ioe) {
            logger.log(LogLevel.HIGH, "Could not open results file.");
        }

        logger.closeLog();

    }

    private void refineResults(boolean contains, String[] refineTokens) {

        ArrayList<LogRow> logRows = new ArrayList<>();

        HtmlReader htmlReader = new HtmlReader(cfg.getResDir() + cfg.getResultsFile(), logRows);

        ArrayList<AuthorResults> authorResultses = new ArrayList<>();
        int totalResults = 0;

        try {

            // for each author's results
            Author author;
            while ((author = htmlReader.findNextAuthor()) != null) {
                String[] searchTokens = htmlReader.getSearchTokens();
                ArrayList<Result> results = new ArrayList<>();
                String resultBlock;
                while ((resultBlock = htmlReader.getNextResult(author)) != null) {
                    Result result = new Result(author, resultBlock, searchTokens);
                    if (result.refine(contains, refineTokens)) {
                        results.add(result);
                    }
                }
                AuthorResults authorResults = new AuthorResults(author, HtmlHelper.printableArray(searchTokens), results);
                authorResultses.add(authorResults);
                totalResults += authorResults.getNumResults();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        htmlReader.close();

        PrintWriter pwResults = null;
        try {

            pwResults = new PrintWriter(cfg.getResDir() + cfg.getResultsFile());
            pwResults.println(HtmlHelper.getResultsHeader("../../mseStyle.css"));

            for (AuthorResults results : authorResultses) {
                results.writeAllResults(pwResults);
            }

            pwResults.println("\n\t\t<div class=\"spaced\">Number of total results: " + totalResults + "</div>");
            pwResults.println(HtmlHelper.getHtmlFooter("\t</div>"));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (pwResults != null) pwResults.close();
        }

    }
}
