/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 *
 * @author michael
 */
public class Config {

    // the number of times a word has to appear before it is too frequent
    public final int TOO_FREQUENT = 10000;

    private String mseVersion;
//    private String defaultBrowser;
    private String workingDir;
    private String resDir;
    private String resultsFileName;
    private String searchString;
    private String searchType;
    private HashMap<Author, Boolean> selectedAuthors;
    private boolean synopsis;
    private boolean beep;
    private boolean splashWindow;
    private boolean autoLoad;
    private boolean fullScan;
    private boolean setup;
    private boolean debugOn;

    public Config() {
        setDefaults();
    }

    private void setDefaults() {

        mseVersion = "3.0.0";
        workingDir = "";
        resDir = "res" + File.separator;
//        defaultBrowser = "/usr/bin/firefox";
//        defaultBrowser = "C:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe";
        resultsFileName = "search_results.htm";
        searchString = "";
        searchType = "Phrase";

        // set the selected books to be searched to only the bible
        selectedAuthors = new HashMap<>();
        for (Author nextAuthor : Author.values()) {
            if (nextAuthor.isSearchable()) {
                selectedAuthors.put(nextAuthor, false);
            }
        }
        selectedAuthors.put(Author.BIBLE, true);

        synopsis = true;
        beep = false;
        splashWindow = false;
        autoLoad = false;
        fullScan = false;
        setup = false;
        debugOn = false;

    }

    public void save(Logger logger) {
        if (!setup) {
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(this);
                File f = new File("config.txt");
                PrintWriter pw = new PrintWriter(f);
                pw.write(json);
                pw.close();
                logger.log(LogLevel.INFO, "Config saved: " + f.getCanonicalPath());
            } catch (IOException ioe) {
                logger.log(LogLevel.LOW, "Could not write config" + ioe.getMessage());
            }
        }
    }

    public void setSetup(boolean setupCheck) {
        setup = setupCheck;
    }
    
    public boolean isSettingUp() {
        return setup;
    }

    public String getMseVersion() {
        return mseVersion;
    }

//    public String getDefaultBrowser() {
//        return defaultBrowser;
//    }
//
//    public void setDefaultBrowser(String defaultBrowser) {
//        this.defaultBrowser = defaultBrowser;
//    }

    public String getWorkingDir() {
        return workingDir;
    }

    public String getResDir() {
        return resDir;
    }

    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    public String getResultsFileName() {
        return resultsFileName;
    }

    public void setResultsFileName(String resultsFileName) {
        this.resultsFileName = resultsFileName;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public HashMap<Author, Boolean> getSelectedAuthors() {
        return selectedAuthors;
    }

    public void setSelectedAuthors(HashMap<Author, Boolean> selectedAuthors) {
        this.selectedAuthors = selectedAuthors;
    }

    public void setSelectedAuthor(Author author, boolean isSelected) {
        selectedAuthors.put(author, isSelected);
    }

    public Boolean getSelectedAuthor(Author author) {
        return selectedAuthors.get(author);
    }

    public boolean isAuthorSelected() {
        boolean check = false;
        for (Author nextAuthor : Author.values()) {
            if (nextAuthor != Author.TUNES) {
                if (getSelectedAuthor(nextAuthor)) {
                    check = true;
                }
            }
        }
        return check;
    }

    public boolean isBeep() {
        return beep;
    }

    public void setBeep(boolean beep) {
        this.beep = beep;
    }

    public boolean isSplashWindow() {
        return splashWindow;
    }

    public void setSplashWindow(boolean splashWindow) {
        this.splashWindow = splashWindow;
    }

    public boolean isAutoLoad() {
        return autoLoad;
    }

    public void setAutoLoad(boolean autoLoad) {
        this.autoLoad = autoLoad;
    }

    public boolean isFullScan() {
        return fullScan;
    }

    public void setFullScan(boolean fullScan) {
        this.fullScan = fullScan;
    }

    public boolean isDebugOn() {
        return debugOn;
    }

    public void setDebugOn(boolean debugOn) {
        this.debugOn = debugOn;
    }

    public boolean isSynopsis() {
        return synopsis;
    }

    public void setSynopsis(boolean synopsis) {
        this.synopsis = synopsis;
    }

}
