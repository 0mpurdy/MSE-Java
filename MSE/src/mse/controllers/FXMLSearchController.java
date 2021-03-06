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
import mse.common.config.Config;
import mse.common.log.LogLevel;
import mse.common.log.Logger;
import mse.data.search.Search;
import mse.data.search.SearchType;
import mse.helpers.FileHelper;
import mse.helpers.HtmlHelper;
import mse.refine.RefineThread;
import mse.search.*;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.GridPane;
import mse.data.author.Author;
import mse.data.Map;
import mse.helpers.OpenFileHandler;

/**
 * @author michael
 */
public class FXMLSearchController implements Initializable {

    private Logger logger;
    private Config cfg;
    private ArrayList<CheckBox> checkboxes;
    private RadioMenuItem defaultSearchScope;
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
    @FXML
    Menu scopeMenu;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // hide the progress bar
        progressBar.setVisible(false);

        // set the progress text to something helpful
        progressLabel.setText("From the menu bar you can select a book to browse.");

        // open new logger
        logger = new Logger(LogLevel.INFO, Logger.DEFAULT_LOG);
        logger.openLog();
        logger.log(LogLevel.INFO, "Started MSE");
        logger.closeLog();
        logger.openLog();

        // try to recover config options
        cfg = new Config(logger);
        cfg.save();

        // set the setup config flag to true
        cfg.setSetup(true);

        setupCheckboxes();

        setupMaps();

        cfg.setSetup(false);

        // initialise the search box
        searchBox.setText(cfg.getSearchString());

        // initialise the index store
        indexStore = new IndexStore(cfg);

        logger.closeLog();
    }

    private void setupCheckboxes() {
        // add checkboxes for searching author
        // TODO check if they're available first
        checkboxes = new ArrayList<>();
        int i = 0;
        for (Author nextAuthor : Author.values()) {
            if (nextAuthor.isSearchable()) {
                CheckBox nextCheckBox = new CheckBox(nextAuthor.getCode());
                nextCheckBox.selectedProperty().addListener((ov, old_val, new_val) -> {
                    if (!cfg.isSettingUp()) {
                        cfg.setSelectedAuthor(nextAuthor.getCode(), new_val);
                        cfg.save();
                    }
                });

                // select if it is to be searched or not
                nextCheckBox.setSelected(cfg.isAuthorSelected(nextAuthor.getCode()));

                checkBoxPane.add(nextCheckBox, i % 6, i / 6);
                checkboxes.add(nextCheckBox);
                i++;
            }

            // add menu items to open contents pages
            MenuItem nextMenuItem = new MenuItem(nextAuthor.getName());
            nextMenuItem.setOnAction(new OpenFileHandler(cfg, logger, cfg.getResDir() + File.separator + FileHelper.getContentsFile(nextAuthor, File.separator)));
            booksMenu.getItems().add(nextMenuItem);
            if ((booksMenu.getItems().size() == 2) || (booksMenu.getItems().size() == Author.values().length - 2)) {
                booksMenu.getItems().add(new SeparatorMenuItem());
            }

        }
    }

    private void setupMaps() {

        File mapsFile = new File(cfg.getResDir() + File.separator + "Maps.txt");

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
                    int logLevelIndex = logLevelMenu.getItems().indexOf(event.getSource());
                    logger.setLogLevel(LogLevel.values()[logLevelIndex]);
                });
                logLevelMenu.getItems().add(nextLevelRadioItem);
            }

            // add search scopes to menu
            ToggleGroup scopeToggleGroup = new ToggleGroup();
            for (SearchType scope : SearchType.values()) {
                RadioMenuItem nextScopeRadioMenuItem = new RadioMenuItem(scope.getMenuName());
                if (scope == cfg.getSearchType()) nextScopeRadioMenuItem.setSelected(true);
                if (scope == SearchType.MATCH) defaultSearchScope = nextScopeRadioMenuItem;
                nextScopeRadioMenuItem.setToggleGroup(scopeToggleGroup);
                nextScopeRadioMenuItem.setOnAction(event -> {
                    int scopeIndex = scopeMenu.getItems().indexOf(event.getSource());
                    cfg.setSearchType(SearchType.values()[scopeIndex]);
                });
                scopeMenu.getItems().add(nextScopeRadioMenuItem);
            }

            // add maps to maps menu
            for (Map map : maps) {
                MenuItem nextMenuItem = new MenuItem(map.getArea() + " - " + map.getMapName());
                nextMenuItem.setOnAction(new OpenFileHandler(cfg, logger, cfg.getResDir() + File.separator + "maps" + File.separator + map.getMapLocation()));
                mapsMenu.getItems().add(nextMenuItem);
                if (mapsMenu.getItems().size() == 3) {
                    mapsMenu.getItems().add(new SeparatorMenuItem());
                }
            }

        } catch (IOException ioe) {
            logger.log(LogLevel.HIGH, "Could not find maps file - " + mapsFile.getAbsolutePath());
        }
    }

    @FXML
    public void handlesSearch(ActionEvent e) {

        logger.openLog();
        progressBar.setVisible(false);

        // save new config
        cfg.setSearchString(searchBox.getText());
        cfg.save();

        String searchString = searchBox.getText();

        if (searchString.equals("")) {
            // check if there is any string to search
            progressLabel.setText("Invalid search string");
            logger.log(LogLevel.INFO, "Invalid search string: " + searchString);
        } else if ((searchString.contains("*")) && (searchString.contains(" "))) {
            // check if it is a valid wild card search
            logger.log(LogLevel.INFO, "Wildcard searches must only have one word");
            progressLabel.setText("Wildcard searches must only have one word");
        } else {
            // check if any authors are selected
            if (cfg.isAnyAuthorSelected()) {

                logger.log(LogLevel.INFO, "Searched: " + searchString + " with type: " + cfg.getSearchType().getMenuName());
                try {
                    addPreviousSearch(searchString);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                // get which authors to search
                HashMap<String, Boolean> authors = cfg.getSelectedAuthors();
                ArrayList<Author> authorsToSearch = new ArrayList<>();
                for (Author nextAuthor : Author.values()) {
                    if (!nextAuthor.isSearchable()) continue;
                    if (authors.get(nextAuthor.getCode())) {
                        authorsToSearch.add(nextAuthor);
                    }
                }

                Search search = new Search(cfg.getSearchType(), searchString);

                progressBar.setVisible(true);
                progressBar.setProgress(0);

                AtomicInteger progress = new AtomicInteger();

                SearchProgressThread searchProgressThread = new SearchProgressThread(progressBar, progressLabel, progress, authorsToSearch.size());
                searchProgressThread.start();

                // start the thread to search
                SearchThread searchThread = new SearchThread(cfg, logger, authorsToSearch, indexStore, search, progress);
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

        logger.openLog();


        String refineText = searchBox.getText();
        logger.log(LogLevel.INFO, "Refined by: " + refineText);

        boolean contains = !(refineText.charAt(0) == '!');

        if (!contains) {
            refineText = refineText.substring(1);
            progressLabel.setText("Search doesn't include \"" + refineText + "\"");
        } else {
            progressLabel.setText("Search includes \"" + refineText + "\"");
        }

        String[] refineTokens = null;

        if (!refineText.equals("")) {
            refineTokens = HtmlHelper.tokenizeLine(refineText);
        }

        RefineThread refineThread = new RefineThread(cfg, logger, contains, refineTokens);
        refineThread.start();

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

    private void addPreviousSearch(String searchString) throws IOException {

        File previousSearchFile = new File(cfg.getPrevSearchesFile());
        PrintWriter previousSearchWriter;
        if (!previousSearchFile.exists()) {
            previousSearchFile.getParentFile().mkdirs();
            previousSearchFile.createNewFile();
            previousSearchWriter = new PrintWriter(cfg.getPrevSearchesFile());
            HtmlHelper.writeHtmlHeader(previousSearchWriter, "Previous Searches", "../../mseStyles.css");
            previousSearchWriter.println("\n<body>\n\t<p>\n\t\t<ul>\n\t\t\t<li>" + searchString);
        } else {
            BufferedReader br = new BufferedReader(new FileReader(previousSearchFile));
            java.util.List<String> previousLines = Files.readAllLines(previousSearchFile.toPath());
            previousSearchWriter = new PrintWriter(cfg.getPrevSearchesFile());
            previousLines.forEach(previousSearchWriter::println);
            previousSearchWriter.println("\t\t\t<li>" + searchString);
        }

        previousSearchWriter.close();
    }

    // region fileMenu

    @FXML
    public void handlesPreviousSearches(ActionEvent e) {
        File previousSearches = new File(cfg.getPrevSearchesFile());
        try {
            if (previousSearches.exists()) {
                Desktop.getDesktop().open(previousSearches);
            } else {
                progressLabel.setText("No previous searches.");
            }
        } catch (IOException | IllegalArgumentException ioe) {
            logger.log(LogLevel.HIGH, "Could not open results file.");
        }
    }

    @FXML
    public void handlesExit(ActionEvent e) {
        System.exit(0);
    }

    // endregion

    // region helpMenu

    @FXML
    public void handlesSearchEngineHelp(ActionEvent e) {
        try {
            File logFile = new File(cfg.getSearchEngineHelpPage());
            Desktop.getDesktop().open(logFile);
        } catch (IOException ioe) {
            logger.log(LogLevel.LOW, "Could not open log file.");
        }
    }

    @FXML
    public void handlesContactSupport(ActionEvent e) {
        try {
            File logFile = new File(cfg.getSupportPage());
            Desktop.getDesktop().open(logFile);
        } catch (IOException ioe) {
            logger.log(LogLevel.LOW, "Could not open log file.");
        }
    }

    @FXML
    public void handlesViewLabourers(ActionEvent e) {
        try {
            File logFile = new File(cfg.getLabourersPage());
            Desktop.getDesktop().open(logFile);
        } catch (IOException ioe) {
            logger.log(LogLevel.LOW, "Could not open log file.");
        }
    }

    @FXML
    public void handlesAboutSearchEngine(ActionEvent e) {
        try {
            File logFile = new File(cfg.getAboutPage());
            Desktop.getDesktop().open(logFile);
        } catch (IOException ioe) {
            logger.log(LogLevel.LOW, "Could not open log file.");
        }
    }

    // endregion

    // region advancedMenu

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
    public void handlesRefreshLog(ActionEvent e) {
        logger.refresh();
    }

    @FXML
    public void handlesRefreshConfig(ActionEvent e) {
        cfg.refresh();

        cfg.setSetup(true);

        for (CheckBox nextCheckBox : checkboxes) {
            nextCheckBox.setSelected(cfg.isAuthorSelected(nextCheckBox.getText()));
        }
        searchBox.setText(cfg.getSearchString());

        defaultSearchScope.setSelected(true);

        cfg.setSetup(false);
    }

    @FXML
    public void handlesRefreshPreviousSearches(ActionEvent e) {
        try {
            File previousSearchFile = new File(cfg.getPrevSearchesFile());
            PrintWriter previousSearchWriter;
            previousSearchFile.getParentFile().mkdirs();
            previousSearchFile.createNewFile();
            previousSearchWriter = new PrintWriter(cfg.getPrevSearchesFile());
            HtmlHelper.writeHtmlHeader(previousSearchWriter, "Previous Searches", "../../mseStyle.css");
            previousSearchWriter.println("\n<body>\n\t<p>\n\t\t<ul>");

            previousSearchWriter.close();

            File resultsFile = new File(cfg.getResDir() + File.separator + cfg.getResultsFile());
            PrintWriter resultsWriter;
            resultsFile.getParentFile().mkdirs();
            resultsFile.createNewFile();
            resultsWriter = new PrintWriter(cfg.getResDir() + File.separator + cfg.getResultsFile());
            HtmlHelper.writeHtmlHeader(resultsWriter, "Results", "../../mseStyle.css");
            resultsWriter.println("\n<body>\n</body>\n</html>");

            resultsWriter.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    // endregion

//    removed to save having to use gson lib
//    private Config readConfig() {
//        try {
//            Gson gson = new Gson();
//            IFileReader fr = new IFileReader("config.txt");
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
