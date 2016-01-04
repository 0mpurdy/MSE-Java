package mse.search;

import mse.common.LogRow;
import mse.data.IResult;

import java.util.ArrayList;

/**
 * Created by Michael on 17/11/2015.
 */
public abstract class SingleSearchThread extends Thread {

    abstract ArrayList<LogRow> getLog();

    abstract ArrayList<IResult> getResults();

    abstract int getNumberOfResults();

}
