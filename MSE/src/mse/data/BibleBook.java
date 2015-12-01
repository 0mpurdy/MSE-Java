package mse.data;

public enum BibleBook {

    GENESIS("Genesis", 50),
    EXODUS("Exodus", 40),
    LEVITICUS("Leviticus", 27),
    NUMBERS("Numbers", 36),
    DEUTERONOMY("Deuteronomy", 34),
    JOSHUA("Joshua", 24),
    JUDGES("Judges", 21),
    RUTH("Ruth", 4),
    SAMUEL1("1Samuel", 31),
    SAMUEL2("2Samuel", 24),
    KINGS1("1Kings", 22),
    KINGS2("2Kings", 25),
    CHRONICLES1("1Chronicles", 29),
    CHRONICLES2("2Chronicles", 36),
    EZRA("Ezra", 10),
    NEHEMIAH("Nehemiah", 13),
    ESTHER("Esther", 10),
    JOB("Job", 42),
    PSALMS("Psalms", 150),
    PROVERBS("Proverbs", 31),
    ECCLESIASTES("Ecclesiastes", 12),
    SONGOFSONGS("SongOfSongs", 8),
    ISAIAH("Isaiah", 66),
    JEREMIAH("Jeremiah", 52),
    LAMENTATIONS("Lamentations", 5),
    EZEKIEL("Ezekiel", 48),
    DANIEL("Daniel", 12),
    HOSEA("Hosea", 14),
    JOEL("Joel", 3),
    AMOS("Amos", 9),
    OBADIAH("Obadiah", 1),
    JONAH("Jonah", 4),
    MICAH("Micah", 7),
    NAHUM("Nahum", 3),
    HABAKKUK("Habakkuk", 3),
    ZEPHANIAH("Zephaniah", 3),
    HAGGAI("Haggai", 2),
    ZECHARIAH("Zechariah", 14),
    MALACHI("Malachi", 4),
    MATTHEW("Matthew", 28),
    MARK("Mark", 16),
    LUKE("Luke", 24),
    JOHN("John", 21),
    ACTS("Acts", 28),
    ROMANS("Romans", 16),
    CORINTHIANS1("1Corinthians", 16),
    CORINTHIANS2("2Corinthians", 13),
    GALATIANS("Galatians", 6),
    EPHESIANS("Ephesians", 6),
    PHILIPPIANS("Philippians", 4),
    COLOSSIANS("Colossians", 4),
    THESSALONIANS1("1Thessalonians", 5),
    THESSALONIANS2("2Thessalonians", 3),
    TIMOTHY1("1Timothy", 6),
    TIMOTHY2("2Timothy", 4),
    TITUS("Titus", 3),
    PHILEMON("Philemon", 1),
    HEBREWS("Hebrews", 13),
    JAMES("James", 5),
    PETER1("1Peter", 5),
    PETER2("2Peter", 3),
    JOHN1("1John", 5),
    JOHN2("2John", 1),
    JOHN3("3John", 1),
    JUDE("Jude", 1),
    REVELATION("Revelation", 22);

    private String name;
    private int numChapters;

    BibleBook(String name, int numChapters) {
        this.name = name;
        this.numChapters = numChapters;
    }

    public String getName() {
        return name;
    }

    public int getNumChapters() {
        return numChapters;
    }

}
