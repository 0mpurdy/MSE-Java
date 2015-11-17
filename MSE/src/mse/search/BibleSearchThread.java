package mse.search;

import mse.common.Config;
import mse.common.ILogger;
import mse.common.LogLevel;
import mse.data.Author;
import mse.data.Search;

import java.util.ArrayList;

/**
 * Created by Michael on 17/11/2015.
 */
public class BibleSearchThread extends Thread {

    private Config cfg;
    private ILogger logger;
    private IndexStore indexStore;
    private Search search;

    // progress fraction per author
    private double fractionPerAuthor;
    private double progress;

    public BibleSearchThread(Config cfg, ILogger logger, IndexStore indexStore, Search search) {
        this.cfg = cfg;
        this.logger = logger;
        this.indexStore = indexStore;
        this.search = search;
    }

    @Override
    public void run() {

        logger.log(LogLevel.DEBUG, "Started search ... ");



    }
}
