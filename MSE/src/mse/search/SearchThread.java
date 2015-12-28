package mse.search;

import mse.common.Config;
import mse.common.ILogger;
import mse.common.LogLevel;
import mse.common.LogRow;
import mse.data.Author;
import mse.data.AuthorIndex;
import mse.data.Search;
import mse.helpers.HtmlHelper;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Michael on 17/11/2015.
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

//        if (authorsToSearch.contains(Author.BIBLE)) {
//
//            // search the bible
//            AuthorSearchCache asc = new AuthorSearchCache(cfg, indexStore.getIndex(logger, Author.BIBLE),search);
//
//            BibleSearchThread bibleSearchThread = new BibleSearchThread(cfg, asc, progress);
//            singleSearchThreads.add(bibleSearchThread);
//
//            bibleSearchThread.start();
//
//        }

        // for each author to be searched
        for (Author nextAuthor : authorsToSearch) {

            if (!nextAuthor.isSearchable()) continue;

//            ArrayList<LogRow> searchLog = new ArrayList<>();
//            searchLogs.add(searchLog);

            AuthorIndex nextAuthorIndex = indexStore.getIndex(logger, nextAuthor);

            AuthorSearchCache nextAsc = new AuthorSearchCache(cfg, nextAuthorIndex, search);

            AuthorSearchThread nextAuthorSearchThread = new AuthorSearchThread(cfg, nextAsc, progress);
            singleSearchThreads.add(nextAuthorSearchThread);

            nextAuthorSearchThread.start();

//            searchAuthor(resultText, nextAuthor, search, indexStore);
//            resultText.add("Number of results for " + nextAuthor.getName() + ": " + search.getNumAuthorResults());
//            search.clearAuthorValues();

        } // end searching each author


        // write the results

        // try to open and write to the results file
        File resultsFile = new File(cfg.getResDir() + cfg.getResultsFileName());
        if (!resultsFile.exists()) {
            resultsFile.getParentFile().mkdirs();
            try {
                resultsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (PrintWriter pwResults = new PrintWriter(resultsFile)) {

            HtmlHelper.getResultsHeaderLines("Results", "../../mseStyle.css").forEach(pwResults::println);

            // join all the threads
            for (SingleSearchThread nextThread : singleSearchThreads) {
                try {
                    nextThread.join();

                    nextThread.getResults().forEach(pwResults::println);
                    nextThread.getLog().forEach(logger::log);
                    search.addAuthorSearchResults(nextThread.getNumberOfResults());

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            pwResults.println("\n\t\t<div class=\"spaced\">Number of total results: " + search.getTotalSearchResults() + "</div>");
            pwResults.println(HtmlHelper.getHtmlFooter("\t</div>"));

        } catch (FileNotFoundException fnfe) {

            logger.log(LogLevel.HIGH, "Could not find results file.");

        }

//        search.setProgress("Done", 1.0);

        progress.set(1000 * authorsToSearch.size() + 1);

        try {
            Desktop.getDesktop().open(new File(cfg.getResDir() + cfg.getResultsFileName()));
        } catch (IOException | IllegalArgumentException ioe) {
            logger.log(LogLevel.HIGH, "Could not open results file.");
        }

        logger.closeLog();

    }

}
