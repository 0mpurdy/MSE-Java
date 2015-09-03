/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author michael
 */
public class Config {

    private String mseVersion;
    private String defaultBrowser;
    private String workingDir;
    private String resultsFileName;
    private String searchString;
    private String searchType;
    private HashMap<Author, Boolean> selectedAuthors;
    private boolean beep;
    private boolean splashWindow;
    private boolean autoLoad;
    private boolean fullScan;
    private boolean loggingActions;
    private boolean setup = false;
    private boolean debugOn;

    public Config() {
        setDefaults();
    }

    private void setDefaults() {

        mseVersion = "3.0.0";
//        workingDir = "/home/michael/Dropbox/MSE/MSE1/res/";
//        defaultBrowser = "/usr/bin/firefox";
        workingDir = "F:\\dev\\Java\\MSE-Java\\WorkingDir\\res\\";
        defaultBrowser = "C:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe";
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
        loggingActions = true;
        beep = false;
        splashWindow = false;
        autoLoad = false;
        fullScan = false;
        setup = false;
        debugOn = false;
    }

    public void save() {
        if (!setup) {
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(this);
                File f = new File("Config.txt");
                PrintWriter pw = new PrintWriter(f);
                pw.write(json);
                pw.close();
                System.out.println("Config saved: " + f.getCanonicalPath());
            } catch (IOException ioe) {
                System.out.println("Could not write config" + ioe.getMessage());
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

    public String getDefaultBrowser() {
        return defaultBrowser;
    }

    public void setDefaultBrowser(String defaultBrowser) {
        this.defaultBrowser = defaultBrowser;
    }

    public String getWorkingDir() {
        return workingDir;
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

    public void writeToLog(String message) {

        if (loggingActions) {
            FileWriter logWriter = null;
            try {
                File logFile = new File(workingDir + "log.txt");
                logWriter = new FileWriter(logFile, true);
                logWriter.write("\n" + message);
            } catch (IOException ioe) {
                // ignore io exception
            } finally {
                if (logWriter != null) {
                    try {
                        logWriter.close();
                    } catch (IOException ex) {
                    }
                }
            }
        }
    }

}
