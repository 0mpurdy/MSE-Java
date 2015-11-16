package mse.data;

import java.io.File;

public enum Author {

    BIBLE(0, "Bible", "Bible", "bible", 66, true),
    HYMNS(1, "Hymns", "Hymns", "hymns", 5, true),
    TUNES(2, "Tunes", "Hymn Tunes", "tunes", 100, false),
    JND(3, "JND", "J.N.Darby", "jnd", 52, true),
    JBS(4, "JBS", "J.B.Stoney",  "jbs", 17, true),
    CHM(5, "CHM", "C.H.Mackintosh", "chm", 18, true),
    FER(6, "FER", "F.E.Raven", "fer", 21, true),
    CAC(7, "CAC", "C.A.Coates", "cac", 37, true),
    JT(8, "JT", "J.Taylor Snr", "jt", 103, true),
    GRC(9, "GRC", "G.R.Cowell", "grc", 88, true),
    AJG(10, "AJG", "A.J.Gardiner", "ajg", 11, true),
    SMC(11, "SMC", "S.McCallum", "smc", 10, true),
    WJH(12, "WJH", "W.J.House", "wjh", 23, true),
    Misc(13, "Misc", "Various Authors", "misc", 26, true);

    private final int index;
    private final String code;
    private final String name;
    private final String folder;
    private final int numVols;
    private final boolean searchable;

    Author(int index, String code, String name, String folder, int numVols, boolean searchable) {
        this.index = index;
        this.code = code;
        this.name = name;
        this.folder = folder;
        this.numVols = numVols;
        this.searchable = searchable;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getPreparePath() {
        return "source" + File.separator + folder + File.separator;
    }

    public String getPreparePath(String filename) {
        return "source" + File.separator + folder + File.separator + filename;
    }

    public String getTargetPath() {
        return "target" + File.separator + folder + File.separator;
    }

    public String getTargetPath(String filename) {
        return "target" + File.separator + folder + File.separator + filename;
    }

    public String getVolumePath(int volumeNumber) {
        return getTargetPath(folder + volumeNumber + ".htm");
    }

    public String getContentsName() {
        return folder + "_contents";
    }

    public String getIndexFilePath() {
        return getTargetPath("index-" + getCode() + ".idx");
    }

    public boolean isSearchable() {
        return searchable;
    }

    public int getNumVols() {
        return numVols;
    }

    public int getIndex() {
        return index;
    }
}
