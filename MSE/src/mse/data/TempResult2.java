package mse.data;

/**
 * Created by Michael Purdy on 30/12/2015.
 *
 * Temporary until results work properly
 */
public class TempResult2 implements IResult {

    private String tempResult;

    public TempResult2(String tempResult) {
        this.tempResult = tempResult;
    }

    @Override
    public String getBlock() {
        return tempResult;
    }
}
