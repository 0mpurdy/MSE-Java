package mse.common;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mj_pu_000 on 28/09/2015.
 */
public class Logger implements ILogger {

    private final String logFilePath = "Log.txt";

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    File loggingFile;
    PrintWriter pwLog;
    LogLevel logLevel;

    public Logger(LogLevel logLevel) {
        loggingFile = new File(logFilePath);
        try {
            if (!loggingFile.exists()) {
                loggingFile.createNewFile();
            }
        } catch(IOException ioe) {
            System.out.println(ioe.getMessage());
        }
        this.logLevel = logLevel;
    }

    public void log(LogLevel logLevel, String message) {
        if (logLevel.value <= this.logLevel.value) {
            Date date = new Date();
            pwLog.printf("%s [%s] - %s\n", logLevel.tag, dateFormat.format(date), message);
        }
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public void openLog() {
        try {
            this.pwLog = new PrintWriter(new BufferedWriter(new FileWriter(loggingFile, true)));
        } catch (FileNotFoundException fnfe) {
            System.out.println("File not found" + loggingFile.getAbsolutePath());
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }

    public void closeLog() {
        this.pwLog.close();
    }

    public void refresh() {
        closeLog();
        loggingFile.delete();
        try {
            loggingFile.createNewFile();
            openLog();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

}
