package mse.common;

/**
 * Created by mj_pu_000 on 28/09/2015.
 */
public enum LogLevel {
    CRITICAL (0), HIGH(1), LOW(2), INFO(3), DEBUG(4);

    int value;
    String tag;

    LogLevel(int value) {
        this.tag = "[" + this.name().toUpperCase() + "]";
        this.value = value;
    }
}
