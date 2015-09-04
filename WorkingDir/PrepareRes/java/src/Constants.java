//package com.asidua.mse;

import java.util.Properties;

public class Constants {
    public static final String MSE_VERSION = "2.3.3";
    public static final boolean bHymnPointers = true;
    public static final boolean bSynopsisLinks = true;

    public static String NEW_LINE =
        ((Properties) System.getProperties()).getProperty("line.separator");
    public static String WORKING_DIR =
        ((Properties) System.getProperties()).getProperty("user.dir");

    public static final String RESULTS_FILE = "htm/search_results.htm";
    public static final String RESULTS_FILE_TMP =
        "htm/refined_search_results.htm";
    public static final String LOG_FILE = "htm/log.htm";

    public static final int TOO_FREQUENT = 10000;

    public static final int NOT_FOUND = -1;
    public static final int INDEX_LOADED = 0;
    public static final int INDEX_RELOADED = 1;
    public static final int INDEX_NOT_LOADED = -1;

    public static final int BIBLE_POS = 0;
    public static final int JND_POS = 1;
    public static final int JBS_POS = 2;
    public static final int CHM_POS = 3;
    public static final int FER_POS = 4;
    public static final int CAC_POS = 5;
    public static final int JT_POS = 6;
    public static final int GRC_POS = 7;
    public static final int AJG_POS = 8;
    public static final int SMC_POS = 9;
    public static final int MISC_POS = 10;
    public static final int HYMNS_POS = 11;
    public static final int MAX_AUTH_POS = 11;
    public static final int TUNES_POS = 12;

    public static final int LAST_HYMNBOOK = 7;

    public static final int LOG_LOW = 0;
    public static final int LOG_MEDIUM = 1;
    public static final int LOG_HIGH = 2;

    static final int MAX_DIGITS_IN_VOL_NUM = 3;
    static final int MAX_DIGITS_IN_PAGE_NUM = 3;

    public static final int PROGRESS_BAR_MAX = 8000;

    static final String[] saBooks =  {"Genesis", "Exodus", "Leviticus",
        "Numbers", "Deuteronomy", "Joshua", "Judges", "Ruth", "1Samuel",
        "2Samuel", "1Kings", "2Kings", "1Chronicles", "2Chronicles", "Ezra",
        "Nehemiah", "Esther", "Job", "Psalm", "Proverbs", "Ecclesiastes",
        "SongofSongs", "Isaiah", "Jeremiah", "Lamentations", "Ezekiel",
        "Daniel", "Hosea", "Joel", "Amos", "Obadiah", "Jonah", "Micah",
        "Nahum", "Habakkuk", "Zephaniah", "Haggai", "Zechariah", "Malachi",
        "Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1Corinthians",
        "2Corinthians", "Galatians", "Ephesians", "Philippians", "Colossians",
        "1Thessalonians", "2Thessalonians", "1Timothy", "2Timothy", "Titus",
        "Philemon", "Hebrews", "James", "1Peter", "2Peter", "1John", "2John",
        "3John", "Jude", "Revelation", "N/A"};

	public static final String[] saAuths =
        {"Bible", "JND", "JBS", "CHM", "FER", "CAC", "JT", "GRC", "AJG", "SMC", "Misc", "Hymns"};

	public static String getAuth(int pos) {
        return saAuths[pos].toLowerCase();
	}

	public static String getAuthDisplay(int pos) {
        return saAuths[pos];
	}

	public static String getBook(int pos) {
        return saBooks[pos];
	}

	public static String getBookDisplay(int pos) {
        char charFirst = saBooks[pos].charAt(0);
        if (Character.isDigit(charFirst)) {
            return charFirst + " " + saBooks[pos].substring(1);
        } else if (pos == 21) {
            return "Song of Songs";
        } else {
            return saBooks[pos];
        }
	}
}//Constants