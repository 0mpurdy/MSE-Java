package com.zerompurdy.mse.data;

import com.zerompurdy.mse.common.config.Config;
import com.zerompurdy.mse.common.log.ILogger;
import com.zerompurdy.mse.common.log.LogLevel;
import com.zerompurdy.mse.common.log.LogRow;
import com.zerompurdy.mse.data.author.Author;
import com.zerompurdy.mse.data.author.AuthorIndex;
import com.zerompurdy.mse.data.search.Search;
import com.zerompurdy.mse.search.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Michael Purdy
 *         Thread to search multiple authors
 */
public class CopyResourcesThread extends Thread {

    private Config cfg;
    private ILogger logger;

    private String src;
    private String dest;

    private AtomicInteger progress;

    //https://stackoverflow.com/a/34252946
    private void copyDir(String src, String dest, boolean overwrite) {
        AtomicInteger filesCount = new AtomicInteger(0);
        try {
            Files.walk(Paths.get(src))
                    .filter(Files::isRegularFile)
                    .forEach(a -> {
                        var currentFileCount = filesCount.incrementAndGet();
                        progress.set(currentFileCount * 1000);

                        Path b = Paths.get(dest, a.toString().substring(src.length()));
                        try {
                            if (!a.toString().equals(src)) {
                                b.toFile().getParentFile().mkdirs();

                                Files.copy(a, b, overwrite ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING} : new CopyOption[]{});
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            //permission issue
            e.printStackTrace();
        }
    }


    public CopyResourcesThread(Config cfg, ILogger logger, AtomicInteger progress, String src, String dest) {
        this.cfg = cfg;
        this.logger = logger;
        this.progress = progress;

        this.src = src;
        this.dest = dest;
    }

    /**
     * Searches each author on individual threads,
     * prints out the aggregated results then
     * opens the results file
     */
    @Override
    public void run() {

        copyDir(this.src, this.dest, true);

        logger.closeLog();
    }

}
