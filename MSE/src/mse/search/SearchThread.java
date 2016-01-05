package mse.search;

import mse.common.Config;
import mse.common.ILogger;
import mse.common.LogLevel;
import mse.common.LogRow;
import mse.data.*;
import mse.helpers.HtmlHelper;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Michael Purdy on 17/11/2015.
 *
 * This is a thread to perform a search across a range of authors
 */
public class SearchThread extends Thread {

    private Config cfg;
    private ILogger logger;
    private IndexStore indexStore;
    private Search search;
    ArrayList<Author> authorsToSearch;

    ArrayList<SingleSearchThread> singleSearchThreads;
    ArrayList<ArrayList<LogRow>> searchLogs;

    // the progress of the current search (0 - 1000)
    private AtomicInteger progress;

    public SearchThread(Config cfg, ILogger logger, ArrayList<Author> authorsToSearch, IndexStore indexStore, Search search, AtomicInteger progress) {
        this.cfg = cfg;
        this.logger = logger;
        this.authorsToSearch = authorsToSearch;
        this.indexStore = indexStore;
        this.search = search;
        this.progress = progress;

        singleSearchThreads = new ArrayList<>();

        searchLogs = new ArrayList<>();
//        this.progress.set(0);
    }

    @Override
    public void run() {

        // for each author to be searched
        for (Author nextAuthor : authorsToSearch) {

            if (!nextAuthor.isSearchable()) continue;

            AuthorIndex nextAuthorIndex = indexStore.getIndex(logger, nextAuthor);

            AuthorSearchCache nextAsc = new AuthorSearchCache(cfg, nextAuthorIndex, search);

            AuthorSearchThread nextAuthorSearchThread = new AuthorSearchThread(cfg, nextAsc, progress);
            singleSearchThreads.add(nextAuthorSearchThread);

            nextAuthorSearchThread.start();

        } // end searching each author


        // write the results

        // try to open and write to the results file
        File resultsFile = new File(cfg.getResDir() + cfg.getResultsFile());
        if (!resultsFile.exists()) {
            resultsFile.getParentFile().mkdirs();
            try {
                resultsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!(resultsFile.setReadable(true, false) && resultsFile.setWritable(true, false))) {
            logger.log(LogLevel.HIGH, "Could not set permissions on results file.");
        }

        try (PrintWriter pwResults = new PrintWriter(resultsFile)) {

            pwResults.println(HtmlHelper.getResultsHeader("../../mseStyle.css"));

            // join all the threads
            for (SingleSearchThread nextThread : singleSearchThreads) {
                try {
                    nextThread.join();

                    AuthorSearchCache asc = ((AuthorSearchThread) nextThread).getAsc();

                    // write the author header
                    pwResults.println(HtmlHelper.getAuthorResultsHeader(asc.author, asc.printableSearchWords()));

                    // write all the results / errors
                    for (IResult result : nextThread.getResults()) {
                        pwResults.println(result.getBlock());
                    }

                    HtmlHelper.closeAuthorContainer(pwResults);

                    // write the number of results for the author
                    pwResults.println(HtmlHelper.getSingleAuthorResults(asc.getAuthorName(), asc.numAuthorResults));

                    // write the log
                    nextThread.getLog().forEach(logger::log);

                    // add the number of search results to the total
                    search.addAuthorSearchResults(nextThread.getNumberOfResults());

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            pwResults.println("\n\t\t<div class=\"spaced\">Number of total results: " + search.getTotalSearchResults() + "</div>");
            pwResults.println(HtmlHelper.getHtmlFooter("\t</div>"));

        } catch (FileNotFoundException fnfe) {
            logger.log(LogLevel.HIGH, "Could not find results file: " + resultsFile.getAbsolutePath());
            logger.logException(fnfe);
        }

        progress.set(1000 * authorsToSearch.size() + 1);

        try {
            Desktop.getDesktop().open(resultsFile);
        } catch (IOException | IllegalArgumentException ioe) {
            logger.log(LogLevel.HIGH, "Desktop could not open results file: " + resultsFile.getAbsolutePath());
            logger.logException(ioe);
        }

        logger.closeLog();

    }

}
