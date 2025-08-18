package com.zerompurdy.mse.refine;

import com.zerompurdy.mse.common.config.Config;
import com.zerompurdy.mse.common.log.ILogger;
import com.zerompurdy.mse.common.log.LogLevel;
import com.zerompurdy.mse.common.log.LogRow;
import com.zerompurdy.mse.data.author.Author;
import com.zerompurdy.mse.data.search.AuthorResults;
import com.zerompurdy.mse.data.search.Result;
import com.zerompurdy.mse.helpers.HtmlHelper;
import com.zerompurdy.mse.helpers.HtmlReader;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
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
            Desktop.getDesktop().open(new File(cfg.getResDir() + File.separator + cfg.getResultsFile()));
        } catch (IOException | IllegalArgumentException ioe) {
            logger.log(LogLevel.HIGH, "Could not open results file.");
        }

        logger.closeLog();

    }

    private void refineResults(boolean contains, String[] refineTokens) {

        ArrayList<LogRow> logRows = new ArrayList<>();

        HtmlReader htmlReader = new HtmlReader(cfg.getResDir() + File.separator + cfg.getResultsFile(), logRows);

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

            pwResults = new PrintWriter(cfg.getResDir() + File.separator + cfg.getResultsFile());
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
