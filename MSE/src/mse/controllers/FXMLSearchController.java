/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse.controllers;

import com.google.gson.reflect.TypeToken;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import mse.VolumeSearch;
import mse.common.*;
import com.google.gson.Gson;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.GridPane;
import mse.data.Map;
import mse.handlers.OpenFileHandler;

/**
 *
 * @author michael
 */
public class FXMLSearchController implements Initializable {

    private Logger logger;
    private Config cfg;

    @FXML
    Button refineButton;
    @FXML
    Menu booksMenu;
    @FXML
    TextField searchBox;
    @FXML
    GridPane checkBoxPane;
    @FXML
    ProgressBar progressBar;
    @FXML
    Label progressLabel;
    @FXML
    Menu mapsMenu;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // hide the progress bar
        progressBar.setVisible(false);

        // open new logger
        logger = new Logger(new File("Log.txt"), LogLevel.INFO);
        logger.openLog();

        // try to recover config options
        if ((cfg = readConfig()) == null) {
            cfg = new Config();
            cfg.save(logger);
        }

        // set the setup config flag to true
        cfg.setSetup(true);

        // add checkboxes for searching author
        // TODO check if they're available first
        int i = 0;
        for (Author nextAuthor : Author.values()) {
            if (nextAuthor.isSearchable()) {
                CheckBox nextCheckBox = new CheckBox(nextAuthor.getCode());
                nextCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
                    public void changed(ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) {
                        if (!cfg.isSettingUp()) {
                            cfg.setSelectedAuthor(nextAuthor, true);
                            cfg.save(logger);
                        }
                    }
                });

                // select if it is to be searched or not
                nextCheckBox.setSelected(cfg.getSelectedAuthor(nextAuthor));

                checkBoxPane.add(nextCheckBox, i % 6, i / 6);
                i++;
            }

            // add menu items to open contents pages
            MenuItem nextMenuItem = new MenuItem(nextAuthor.getName());
            nextMenuItem.setOnAction(new OpenFileHandler(cfg, logger, cfg.getResDir() + nextAuthor.getTargetPath(nextAuthor.getContentsName() + ".htm")));
            booksMenu.getItems().add(nextMenuItem);
            if ((booksMenu.getItems().size() == 2) || (booksMenu.getItems().size() == Author.values().length - 2)) {
                booksMenu.getItems().add(new SeparatorMenuItem());
            }

        }

        // add menu items for maps
        try {
            File f = new File(cfg.getResDir() + "maps.txt");
            java.util.List<String> jsonLines = Files.readAllLines(Paths.get(f.toURI()));
            String json = "";
            for (String line : jsonLines) {
                json+= line;
            }
            Gson gson = new Gson();
            ArrayList<Map> maps = gson.fromJson(json, new TypeToken<ArrayList<Map>>(){}.getType());
            for (Map map : maps) {
                MenuItem nextMenuItem = new MenuItem(map.getArea() + " - " + map.getMapName());
                nextMenuItem.setOnAction(new OpenFileHandler(cfg, logger, cfg.getResDir() + "maps" + File.separator + map.getMapLocation()));
                mapsMenu.getItems().add(nextMenuItem);
                if (mapsMenu.getItems().size() == 3) {
                    mapsMenu.getItems().add(new SeparatorMenuItem());
                }
            }
        } catch (IOException ioe) {
            logger.log(LogLevel.HIGH, "Could not find maps file.");
            logger.log(LogLevel.HIGH, ioe.getMessage());
        }
        
        cfg.setSetup(false);

        // initialise the search box
        searchBox.setText(cfg.getSearchString());

        logger.closeLog();
    }

    @FXML
    public void handlesSearch(ActionEvent e) {

        logger.openLog();

        // save new config
        cfg.setSearchString(searchBox.getText());
        cfg.save(logger);

        String searchString = searchBox.getText();

        if ((searchString.contains("*")) && (searchString.contains(" "))) {
            logger.log(LogLevel.INFO, "Wildcard searches must only have one word");
            progressLabel.setText("Wildcard searches must only have one word");
        } else if (searchString.equals("")) {
            progressLabel.setText("Invalid search string");
            logger.log(LogLevel.INFO, "Invalid search string: " + searchString);
        } else {
            // if any authors are selected            
            if (cfg.isAuthorSelected()) {
                // TODO progress window
                logger.log(LogLevel.INFO, "Searched: " + searchString);

                // get which authors to search
                HashMap<Author, Boolean> authors = cfg.getSelectedAuthors();
                ArrayList authorsToSearch = new ArrayList();
                for (Author nextAuthor : Author.values()) {
                    if (authors.get(nextAuthor)) {
                        authorsToSearch.add(nextAuthor);
                    }
                }

                // start the thread to search
                Thread search = new Thread(new VolumeSearch(cfg,logger,searchString, authorsToSearch));
                search.start();
            } else {
                progressLabel.setText("At least one author must be selected");
                logger.log(LogLevel.INFO, "At least one author must be selected");
            }
        }

        logger.closeLog();
    }

    @FXML
    public void handlesRefine(ActionEvent e) {
    }

    @FXML
    public void handlesViewLogFile(ActionEvent e) {
        try {
            File logFile = new File(cfg.getWorkingDir() + "log.txt");
            Desktop.getDesktop().open(logFile);
        } catch (IOException ioe) {
            logger.log(LogLevel.LOW, "Could not open log file.");
        }
    }

    @FXML
    public void handlesViewConfigFile(ActionEvent e) {
        try {
            File logFile = new File(cfg.getWorkingDir() + "config.txt");
            Desktop.getDesktop().open(logFile);
        } catch (IOException ioe) {
            logger.log(LogLevel.LOW, "Could not open log file.");
        }
    }

    private void updateSelectedAuthor(ItemEvent itemEvent, Author author) {
        cfg.setSelectedAuthor(author, true);
    }

    private Config readConfig() {
        try {
            Gson gson = new Gson();
            FileReader fr = new FileReader("config.txt");
            Path jsonPath = Paths.get("config.txt");
            String json = new String(Files.readAllBytes(jsonPath));
            logger.log(LogLevel.INFO, "Config loaded.");
            return gson.fromJson(json, Config.class);
        } catch (IOException fnfe) {
            logger.log(LogLevel.LOW, "Config file could not be read.");
            return null;
        }
    }

}
