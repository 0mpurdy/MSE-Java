package mse.common;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mj_pu_000 on 28/09/2015.
 */
public class Logger {

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    File loggingFile;
    PrintWriter pwLog;
    LogLevel logLevel;

    public Logger(File loggingFile, LogLevel logLevel) {
        this.loggingFile = loggingFile;
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

}
