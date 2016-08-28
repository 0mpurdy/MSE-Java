package mse.common.log;

/**
 * @author Michael Purdy
 */
public class LogRow {

    public LogLevel logLevel;
    public String message;

    public LogRow(LogLevel logLevel, String message) {
        this.logLevel = logLevel;
        this.message = message;
    }

}
