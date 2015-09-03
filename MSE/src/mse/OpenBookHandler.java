/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;

/**
 *
 * @author michael
 */
public class OpenBookHandler implements EventHandler<ActionEvent> {

    private HtmlBuilder htmlB;
    private Config cfg;
    
    public OpenBookHandler(Config cfg, HtmlBuilder htmlB) {
        this.cfg = cfg;
        this.htmlB = htmlB;
    }
    
    @Override
    public void handle(ActionEvent e) {
        String result = ((MenuItem) e.getSource()).getText();
        String indexName = "Error";
        
        // check which author was selected
        for (Author nextAuthor : Author.values()) {
            if (result.equalsIgnoreCase(nextAuthor.getName())){
                indexName = cfg.getWorkingDir() + nextAuthor.getContentsPath();
            }
        }
        
        try {
            htmlB.openPageExternally(cfg, indexName);
            cfg.writeToLog("Opened: " + indexName);
        } catch (IOException ex) {
            cfg.writeToLog("Could not locate: " + indexName);
            cfg.writeToLog(ex.getMessage());
        }
    }
    
}
