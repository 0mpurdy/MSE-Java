package com.zerompurdy.mse.search;

import com.zerompurdy.mse.common.config.Config;
import com.zerompurdy.mse.common.log.ILogger;
import com.zerompurdy.mse.common.log.LogLevel;
import com.zerompurdy.mse.common.log.LogRow;
import com.zerompurdy.mse.data.author.Author;
import com.zerompurdy.mse.data.author.AuthorIndex;
import com.zerompurdy.mse.data.search.Search;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Michael Purdy
 *         Thread to search multiple authors
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

    /**
     * Searches each author on individual threads,
     * prints out the aggregated results then
     * opens the results file
     */
    @Override
    public void run() {

        // search each author on an individual thread
        for (Author nextAuthor : authorsToSearch) {

            if (!nextAuthor.isSearchable()) continue;

            AuthorIndex nextAuthorIndex = indexStore.getIndex(logger, nextAuthor);

            AuthorSearchCache nextAsc = new AuthorSearchCache(cfg, nextAuthorIndex, search);

            AuthorSearchThread nextAuthorSearchThread = new AuthorSearchThread(cfg, nextAsc, progress);
            singleSearchThreads.add(nextAuthorSearchThread);

            nextAuthorSearchThread.start();

        } // end searching each author

        // print out the results
        ResultsPrinter resultsPrinter = new ResultsPrinter(cfg, logger);
        File resultsFile = resultsPrinter.print(search, singleSearchThreads);

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
