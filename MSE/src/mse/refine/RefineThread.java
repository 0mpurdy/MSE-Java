package mse.refine;

import mse.common.Config;
import mse.common.LogRow;
import mse.helpers.HtmlReader;

import java.util.ArrayList;

/**
 * Created by michaelpurdy on 28/12/2015.
 */
public class RefineThread extends Thread {

    private Config cfg;

    public RefineThread(Config cfg) {
        this.cfg = cfg;
    }


    @Override
    public void run() {

        ArrayList<LogRow> logRows = new ArrayList<>();

        HtmlReader htmlReader = new HtmlReader(cfg.getResultsFilePath(), logRows);



    }
}
