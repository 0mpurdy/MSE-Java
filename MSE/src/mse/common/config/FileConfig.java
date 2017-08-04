package mse.common.config;

import java.io.File;

/**
 * Created by mj_pu_000 on 30/07/2017.
 */
public class FileConfig {

    private String workingDir;
    private String resDir;
    private String resultsFileName;

    // possibly make these configurable in the future
    private static final String sourceFolder = "source";
    private static final String targetFolder = "target";
    private static final String inputSuffix = ".txt";
    private static final String outputSuffix = ".html";

    public FileConfig() {
        setDefaults();
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public String getResDir() {
        return resDir;
    }

    public String getResultsFileName() {
        return resultsFileName;
    }

    public static String getSourceFolder() {
        return sourceFolder;
    }

    public static String getTargetFolder() {
        return targetFolder;
    }

    public static String getInputSuffix() {
        return inputSuffix;
    }

    public static String getOutputSuffix() {
        return outputSuffix;
    }

    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    public void setResDir(String resDir) {
        // remove unnecessary trailing /
        if (resDir.endsWith("/") || resDir.endsWith("\\")) {
            resDir = resDir.substring(0, resDir.length() - 1);
        }
        this.resDir = resDir;
    }

    public void setResultsFileName(String resultsFileName) {
        this.resultsFileName = resultsFileName;
    }

    private void setDefaults() {
        resDir = "res";
        resultsFileName = "search-results.html";
    }
}
