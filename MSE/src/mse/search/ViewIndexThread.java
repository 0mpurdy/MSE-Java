package mse.search;

import mse.common.Config;
import mse.common.LogLevel;
import mse.common.Logger;
import mse.data.Author;
import mse.data.AuthorIndex;
import mse.helpers.HtmlHelper;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by mj_pu_000 on 05/12/2015.
 */
public class ViewIndexThread extends Thread {

    Config cfg;
    Logger logger;
    ArrayList<Author> authorsToSearch;
    IndexStore indexStore;

    public ViewIndexThread(Config cfg, Logger logger, ArrayList<Author> authorsToSearch, IndexStore indexStore) {
        this.cfg = cfg;
        this.authorsToSearch = authorsToSearch;
        this.logger = logger;
        this.indexStore = indexStore;
    }

    @Override
    public void run() {

        File indexFile = new File(cfg.getResDir() + cfg.getResultsFilePath() + File.separator + "Indexes.htm");
        if (!indexFile.exists()) {
            indexFile.getParentFile().mkdirs();
            try {
                indexFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {

            PrintWriter pw = new PrintWriter(indexFile);

            HtmlHelper.writeHtmlHeader(pw, "Indexes", "../../mseStyle.css");

            pw.println("<body>");

            for (Author author : authorsToSearch) {

                AuthorIndex authorIndex = indexStore.getIndex(logger, author);

                printAuthorIndex(pw, authorIndex);
            }

            pw.println("</body>\n\n</html>");

            pw.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        try {
            Desktop.getDesktop().open(indexFile);
        } catch (IOException | IllegalArgumentException ioe) {
            logger.log(LogLevel.HIGH, "Could not open results file.");
        }

        logger.closeLog();

    }

    private void printAuthorIndex(PrintWriter pw, AuthorIndex authorIndex) {

        pw.println("\t<div class=\"volume-title\">" + authorIndex.getAuthorName() + "</div>");

        int i=0;

        for (Map.Entry<String, Integer> entry : authorIndex.getTokenCountMap().entrySet()) {

            if (i % 12 == 0) {
                pw.println("\t<div class=\"row\">");
            }

            pw.println("\t<div class=\"col-md-2\">\n\t\t<div><strong>" + entry.getKey() + "</strong></div>" +
                    "\n\t\t<div>" + entry.getValue() + "</div>\n\t</div>");

            if (i % 12 == 11) {
                pw.println("\t</div>");
            }

            i++;
        }

        if (i % 12 != 11) {
            pw.println("\t</div>");
        }

    }


}
