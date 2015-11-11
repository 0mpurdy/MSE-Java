/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse.controllers;

import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import mse.data.Search;
import mse.search.AuthorSearch;
import mse.common.*;

import java.awt.*;
import java.beans.EventHandler;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.GridPane;
import mse.data.Author;
import mse.data.Map;
import mse.handlers.OpenFileHandler;
import mse.search.IndexStore;

/**
 *
 * @author michael
 */
public class FXMLSearchController implements Initializable {

    private Logger logger;
    private Config cfg;
    private ArrayList<CheckBox> checkboxes;
    private IndexStore indexStore;

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
    @FXML
    Menu logLevelMenu;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // hide the progress bar
//        progressBar.setVisible(false);

        // open new logger
        logger = new Logger(LogLevel.INFO);
        logger.openLog();

        // try to recover config options
        cfg = new Config(logger);
        cfg.save();

        // set the setup config flag to true
        cfg.setSetup(true);

        // add checkboxes for searching author
        // TODO check if they're available first
        checkboxes = new ArrayList<>();
        int i = 0;
        for (Author nextAuthor : Author.values()) {
            if (nextAuthor.isSearchable()) {
                CheckBox nextCheckBox = new CheckBox(nextAuthor.getCode());
                nextCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
                    public void changed(ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) {
                        if (!cfg.isSettingUp()) {
                            cfg.setSelectedAuthor(nextAuthor.getCode(), new_val);
                            cfg.save();
                        }
                    }
                });

                // select if it is to be searched or not
                nextCheckBox.setSelected(cfg.getSelectedAuthor(nextAuthor.getCode()));

                checkBoxPane.add(nextCheckBox, i % 6, i / 6);
                checkboxes.add(nextCheckBox);
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

        File mapsFile = new File(cfg.getResDir() + "Maps.txt");

        // add menu items for maps
        try (BufferedReader br = new BufferedReader(new FileReader(mapsFile))) {

            ArrayList<Map> maps = new ArrayList<>();
            String area;
            String name;
            String location;
            Map nextMap;

            boolean eof = false;

            while (!eof) {
                area = br.readLine();
                name = br.readLine();
                location = br.readLine();
                br.readLine();

                if ((area == null) || (name == null) || (location == null)) {
                    eof = true;
                } else {
                    nextMap = new Map(area, name, location);
                    maps.add(nextMap);
                }
            }

            // replaced to allow gson dependency to be removed
//            File f = new File(cfg.getResDir() + "maps.txt");
//            java.util.List<String> jsonLines = Files.readAllLines(Paths.get(f.toURI()));
//            String json = "";
//            for (String line : jsonLines) {
//                json+= line;
//            }
//            Gson gson = new Gson();
//            ArrayList<Map> maps = gson.fromJson(json, new TypeToken<ArrayList<Map>>(){}.getType())

            // add log levels to log level menu
            ToggleGroup logLevelToggleGroup = new ToggleGroup();
            for (LogLevel nextLogLevel : LogLevel.values()) {
                RadioMenuItem nextLevelRadioItem = new RadioMenuItem(nextLogLevel.name());
                if (nextLogLevel == LogLevel.INFO) nextLevelRadioItem.setSelected(true);
                nextLevelRadioItem.setToggleGroup(logLevelToggleGroup);
                nextLevelRadioItem.setOnAction(event -> {
                    int i1 = logLevelMenu.getItems().indexOf(event.getSource());
                    logger.setLogLevel(LogLevel.values()[i1]);
                });
                logLevelMenu.getItems().add(nextLevelRadioItem);
            }

            // add maps to maps menu
            for (Map map : maps) {
                MenuItem nextMenuItem = new MenuItem(map.getArea() + " - " + map.getMapName());
                nextMenuItem.setOnAction(new OpenFileHandler(cfg, logger, cfg.getResDir() + "maps" + File.separator + map.getMapLocation()));
                mapsMenu.getItems().add(nextMenuItem);
                if (mapsMenu.getItems().size() == 3) {
                    mapsMenu.getItems().add(new SeparatorMenuItem());
                }
            }

        } catch (IOException ioe) {
            logger.log(LogLevel.HIGH, "Could not find maps file - " + mapsFile.getAbsolutePath());
        }

        cfg.setSetup(false);

        // initialise the search box
        searchBox.setText(cfg.getSearchString());

        // initialise the index store
        indexStore = new IndexStore(cfg);

        logger.closeLog();
    }

    @FXML
    public void handlesSearch(ActionEvent e) {

        logger.openLog();
        progressBar.setVisible(false);

        // save new config
        cfg.setSearchString(searchBox.getText());
        cfg.save();

        String searchString = searchBox.getText();

        if ((searchString.contains("*")) && (searchString.contains(" "))) {
            logger.log(LogLevel.INFO, "Wildcard searches must only have one word");
            progressLabel.setText("Wildcard searches must only have one word");
        } else if (searchString.equals("")) {
            progressLabel.setText("Invalid search string");
            logger.log(LogLevel.INFO, "Invalid search string: " + searchString);
        } else {
            // if any authors are selected
            if (cfg.isAnyAuthorSelected()) {
                // TODO progress window
                logger.log(LogLevel.INFO, "Searched: " + searchString);

                // get which authors to search
                HashMap<String, Boolean> authors = cfg.getSelectedAuthors();
                ArrayList<Author> authorsToSearch = new ArrayList<>();
                for (Author nextAuthor : Author.values()) {
                    if (!nextAuthor.isSearchable()) continue;
                    if (authors.get(nextAuthor.getCode())) {
                        authorsToSearch.add(nextAuthor);
                    }
                }

                Search search = new Search(cfg, logger, searchString, progressBar, progressLabel);
                progressBar.setVisible(true);
                progressBar.setProgress(0);

                // TODO change running runnable back to starting thread
//                new AuthorSearch(cfg,logger,searchString, authorsToSearch,indexStore, search).run();

                // start the thread to search
                AuthorSearch searchThread = new AuthorSearch(cfg,logger,searchString, authorsToSearch,indexStore, search);
                searchThread.start();

            } else {
                progressLabel.setText("At least one author must be selected");
                logger.log(LogLevel.INFO, "At least one author must be selected");
                logger.closeLog();
            }
        }
    }

    @FXML
    public void handlesRefine(ActionEvent e) {
    }

    @FXML
    public void handlesSelectAll(ActionEvent e) {

        boolean allSelected = true;

        for (CheckBox nextCheckBox : checkboxes) {
            if (!nextCheckBox.isSelected()) {
                allSelected = false;
                nextCheckBox.setSelected(true);
            }
        }

        if (allSelected) {
            for (CheckBox nextCheckBox : checkboxes) {
                nextCheckBox.setSelected(false);
            }
        }
    }

    @FXML
    public void handlesViewLogFile(ActionEvent e) {
        try {
            File logFile = new File("Log.txt");
            Desktop.getDesktop().open(logFile);
        } catch (IOException ioe) {
            logger.log(LogLevel.LOW, "Could not open log file.");
        }
    }

    @FXML
    public void handlesViewConfigFile(ActionEvent e) {
        try {
            File logFile = new File("Config.txt");
            Desktop.getDesktop().open(logFile);
        } catch (IOException ioe) {
            logger.log(LogLevel.LOW, "Could not open log file.");
        }
    }

    @FXML
    public void handlesRefreshLogAndConfig(ActionEvent e) {
        logger.refresh();
        cfg.refresh();
        logger.closeLog();

        cfg.setSetup(true);

        for (CheckBox nextCheckBox : checkboxes) {
            nextCheckBox.setSelected(cfg.getSelectedAuthor(nextCheckBox.getText()));
        }
        searchBox.setText(cfg.getSearchString());

        cfg.setSetup(false);
    }

//    removed to save having to use gson lib
//    private Config readConfig() {
//        try {
//            Gson gson = new Gson();
//            FileReader fr = new FileReader("config.txt");
//            Path jsonPath = Paths.get("config.txt");
//            String json = new String(Files.readAllBytes(jsonPath));
//            logger.log(LogLevel.INFO, "Config loaded.");
//            return gson.fromJson(json, Config.class);
//        } catch (IOException fnfe) {
//            logger.log(LogLevel.LOW, "Config file could not be read.");
//            return null;
//        }
//    }

}
