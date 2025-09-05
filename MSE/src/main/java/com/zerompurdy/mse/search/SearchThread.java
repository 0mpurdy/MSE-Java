package mse.search;

import mse.common.config.Config;
import mse.common.log.ILogger;
import mse.common.log.LogLevel;
import mse.common.log.LogRow;
import mse.data.author.Author;
import mse.data.author.AuthorIndex;
import mse.data.search.IResult;
import mse.data.search.Search;
import mse.helpers.HtmlHelper;

import java.awt.*;
import java.io.*;
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
