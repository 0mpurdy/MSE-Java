package com.zerompurdy.mse.helpers;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import com.zerompurdy.mse.common.config.Config;
import com.zerompurdy.mse.common.log.LogLevel;
import com.zerompurdy.mse.common.log.Logger;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

/**
 * Created by mj_pu_000 on 24/10/2015.
 */
public class OpenFileHandler implements EventHandler<ActionEvent> {

    //    private HtmlBuilder htmlB;
    private Config cfg;
    private Logger logger;
    private String path;

    public OpenFileHandler(Config cfg, Logger logger, String path) {
        this.cfg = cfg;
        this.logger = logger;
        this.path = path;
    }

    @Override
    public void handle(ActionEvent e) {
        try {
            Desktop.getDesktop().open(new File(path));
            logger.log(LogLevel.DEBUG, "Opened: " + path);
        } catch (IOException | IllegalArgumentException ex) {
            logger.openLog();
            logger.log(LogLevel.HIGH, "Could not open: " + path);
            logger.log(LogLevel.HIGH, ex.getMessage());
            logger.closeLog();
        }
    }

}