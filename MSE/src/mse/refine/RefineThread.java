package mse.refine;

import mse.common.Config;
import mse.common.ILogger;
import mse.common.LogRow;
import mse.data.Author;
import mse.data.Result;
import mse.helpers.HtmlHelper;
import mse.helpers.HtmlReader;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Michael Purdy on 28/12/2015.
 *
 * Thread for performing the refine function
 */
public class RefineThread extends Thread {

    private Config cfg;
    private ILogger logger;

    public RefineThread(Config cfg, ILogger logger) {
        this.cfg = cfg;
        this.logger = logger;
    }


    @Override
    public void run() {

        ArrayList<LogRow> logRows = new ArrayList<>();

        HtmlReader htmlReader = new HtmlReader(cfg.getResDir() + cfg.getResultsFileName(), logRows);

        try {

            // for each author'a results
            Author author;
            while ((author = htmlReader.findNextAuthor()) != null) {

                String resultBlock;
                while ((resultBlock = htmlReader.getNextResult(author)) != null) {
                    Result result = new Result(author, resultBlock, new String[]{"EMMANUEL"});
                    result.print();
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        htmlReader.close();

        logger.closeLog();

    }
}
