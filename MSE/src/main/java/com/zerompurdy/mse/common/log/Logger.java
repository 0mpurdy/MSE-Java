package com.zerompurdy.mse.common.log;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mj_pu_000 on 28/09/2015.
 */
public class Logger implements ILogger {

    public static final String DATA_DIRECTORY = System.getenv("APPDATA") + File.separator + "MSE";
    public static final String DEFAULT_LOG = DATA_DIRECTORY + File.separator + "log.txt";

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    File loggingFile;
    PrintWriter pwLog;
    LogLevel logLevel;

    public Logger(LogLevel logLevel, String logFilePath) {
        System.out.println("Setting up log file " + logFilePath);


        loggingFile = new File(logFilePath);
        try {
            Path path = Paths.get(DATA_DIRECTORY);
            Files.createDirectories(path);
            if (!loggingFile.exists()) {
                loggingFile.createNewFile();
            }
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
        this.logLevel = logLevel;
    }

    public synchronized void log(LogLevel logLevel, String message) {
        if (logLevel.value <= this.logLevel.value) {
            Date date = new Date();
            String tag = logLevel.tag;
            pwLog.printf("%s [%s] - %s%s", tag, dateFormat.format(date), message, System.lineSeparator());
        }
    }

    public synchronized void log(LogRow logRow) {
        if (logRow.logLevel.value <= this.logLevel.value) {
            Date date = new Date();
            String tag = logRow.logLevel.tag;
            pwLog.printf("%s [%s] - %s\n", tag, dateFormat.format(date), logRow.message);
        }
    }

    public void flush() {
        pwLog.flush();
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

    @Override
    public void logException(Exception e) {
        log(LogLevel.HIGH, e.getMessage());
        e.printStackTrace(pwLog);
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
