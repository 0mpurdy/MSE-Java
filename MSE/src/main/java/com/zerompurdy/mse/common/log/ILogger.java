package mse.common.log;

/**
 * @author Michael Purdy
 */
public interface ILogger {

    void log(LogLevel logLevel, String message);

    void log(LogRow logRow);

    void closeLog();

    void logException(Exception e);

}
