package mse.common;

/**
 * Created by Michael on 17/11/2015.
 */
public class LogRow {

    public LogLevel logLevel;
    public String message;

    public LogRow(LogLevel logLevel, String message) {
        this.logLevel = logLevel;
        this.message = message;
    }

}
