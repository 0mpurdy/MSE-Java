package mse.common.log;

/**
 * @author Michael Purdy
 */
public enum LogLevel {

    CRITICAL(0, "[CRITICAL]"),
    HIGH(1, "[HIGH    ]"),
    LOW(2, "[LOW     ]"),
    INFO(3, "[INFO    ]"),
    DEBUG(4, "[DEBUG   ]"),
    TRACE(5, "[TRACE   ]");

    int value;
    String tag;

    LogLevel(int value, String tag) {
        this.tag = tag;
        this.value = value;
    }

}
