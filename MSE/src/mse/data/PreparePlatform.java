package mse.data;

import mse.helpers.FileHelper;
import java.io.File;

/**
 * Created by Michael on 12/12/2015.
 */
public enum PreparePlatform {

    PC("PC", ".." + File.separator + "MSE-Res-Lite" + File.separator + "res", "target", "../../mseStyle.css", "", false),
    ANDROID("Android", ".." + File.separator + "MSE-Res-Lite" + File.separator + "android-res", "files/target", "../../mseStyle.css", "mse:", false);

    private String name;
    private String res;
    private String targetFolder;

    PreparePlatform(String name, String res, String targetFolder, String stylesLink, String linkPrefix, boolean fullLink) {
        this.name = name;
        this.res = res;
        this.targetFolder = targetFolder;
    }

    public String getName() {
        return name;
    }

    public String getResDir() {
        return res;
    }

    public String getSourcePath() {
        return ".." + File.separator + "MSE-Res-Lite" + File.separator + "res" + File.separator + "source" + File.separator;
    }

    public String getTargetPath() {
        return res + File.separator + targetFolder;
    }
}

