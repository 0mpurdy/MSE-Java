package mse.handlers;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import mse.common.Author;
import mse.common.Config;
import mse.common.LogLevel;
import mse.common.Logger;

import java.awt.*;
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
            System.out.println("Opened: " + path);
        } catch (IOException | IllegalArgumentException ex) {
            logger.openLog();
            logger.log(LogLevel.HIGH, "Could not open: " + path);
            logger.log(LogLevel.HIGH, ex.getMessage());
            logger.closeLog();
        }
    }

}