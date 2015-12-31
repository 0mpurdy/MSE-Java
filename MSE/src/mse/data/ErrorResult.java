package mse.data;

/**
 * Created by michaelpurdy on 30/12/2015.
 */
public class ErrorResult implements IResult {

    private String error;

    public ErrorResult(String error) {
        this.error = error;
    }

    @Override
    public String getBlock() {
        return error;
    }
}
