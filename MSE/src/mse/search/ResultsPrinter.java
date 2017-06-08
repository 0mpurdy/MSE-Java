package mse.search;

import mse.common.config.Config;
import mse.common.log.ILogger;
import mse.common.log.LogLevel;
import mse.data.search.IResult;
import mse.data.search.Search;
import mse.helpers.HtmlHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Prints out the results
 */
public class ResultsPrinter {

    Config cfg;
    ILogger logger;

    public ResultsPrinter(Config cfg, ILogger logger) {
        this.cfg = cfg;
        this.logger = logger;
    }

    /**
     * Print out the results of the searches
     * @param singleSearchThreads threads that complete the search for a single author
     */
    public File print(Search search, ArrayList<SingleSearchThread> singleSearchThreads) {

        // try to open and write to the results file
        File resultsFile = new File(cfg.getResDir() + File.separator + cfg.getResultsFile());
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

        return resultsFile;
    }
}
