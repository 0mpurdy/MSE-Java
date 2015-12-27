package mse.search;

import mse.common.LogRow;

import java.util.ArrayList;

/**
 * Created by Michael on 17/11/2015.
 */
public abstract class SingleSearchThread extends Thread {

    abstract ArrayList<LogRow> getLog();

    abstract ArrayList<String> getResults();

    abstract int getNumberOfResults();

}
