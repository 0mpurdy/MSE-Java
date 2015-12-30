package mse.refine;

import mse.common.Config;
import mse.common.ILogger;
import mse.common.LogRow;
import mse.data.Author;
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

                String result;
                while ((result = htmlReader.getNextResult(author)) != null) {

                    switch (author) {
                        case BIBLE:
                            String[] bibleResults = HtmlHelper.extractBibleResult(result);
                            System.out.println("\nBible Result:\n" + bibleResults[0] + "\n" + bibleResults[1]);
                            break;
                        case HYMNS:
                            System.out.println("\nHymns Result:\n" + HtmlHelper.extractHymnsResult(result));
                            break;
                        default:
                            System.out.println("\n" + author.getCode() + "Result:\n" + HtmlHelper.extractMinistryResult(result));
                    }
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        htmlReader.close();

        logger.closeLog();

    }
}
