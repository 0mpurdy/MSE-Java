package mse.search;

import mse.common.Config;
import mse.common.ILogger;
import mse.common.LogLevel;
import mse.common.LogRow;
import mse.data.Author;
import mse.data.Search;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Michael on 17/11/2015.
 */
public class BibleSearchThread extends SingleSearchThread {

    private Config cfg;
    private ILogger logger;

    private ArrayList<LogRow> searchLog;
    private ArrayList<String> authorResults;

    AuthorSearchCache asc;

    private AtomicInteger progress;

    public BibleSearchThread(Config cfg, AuthorSearchCache asc, AtomicInteger progress) {
        this.cfg = cfg;
        this.searchLog = new ArrayList<>();
        this.authorResults = new ArrayList<>();
        this.progress = progress;

        this.asc = asc;
    }

    @Override
    public void run() {

        searchBible(authorResults, asc);

        authorResults.add("Number of results for " + asc.getAuthorName() + ": " + asc.numAuthorResults);

    }

    private void searchBible(ArrayList<String> authorResults, AuthorSearchCache asc){

    }

    @Override
    ArrayList<LogRow> getLog() {
        return searchLog;
    }

    @Override
    ArrayList<String> getResults() {
        return authorResults;
    }

    @Override
    int getNumberOfResults() {
        return asc.numAuthorResults;
    }
}
