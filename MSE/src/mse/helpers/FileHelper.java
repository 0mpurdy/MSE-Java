package mse.helpers;

import mse.common.config.Config;
import mse.data.author.Author;
import mse.data.author.BibleBook;
import mse.data.author.HymnBook;

/**
 * @author Michael Purdy
 *         Helps to generate links and folder paths
 */
public abstract class FileHelper {

    /**
     * Get the path to an html file, relative to the res directory
     * eg target/fer/fer4.html
     *
     * @param author author of the file
     * @param volNum volume number of the file
     * @return the name of the file
     */
    public static String getHtmlFilePath(Author author, int volNum, String fileSeparator) {
        return getTargetFolder(author, fileSeparator) + fileSeparator + getHtmlFileName(author, volNum);
    }

    /**
     * Get the path to the target folder of an author, relative to the res directory
     * eg target/fer
     *
     * @param author
     * @return
     */
    public static String getTargetFolder(Author author, String fileSeparator) {
        return "target" + fileSeparator + author.getFolder();
    }

    /**
     * Get the name of an html file
     * eg fer4.html
     *
     * @param author author of the file
     * @param volNum volume number of the file
     * @return the name of the file
     */
    public static String getHtmlFileName(Author author, int volNum) {
        String filename;

        switch (author) {
            case BIBLE:
                filename = BibleBook.values()[volNum - 1].getTargetFilename();
                break;
            case HYMNS:
                filename = HymnBook.values()[volNum - 1].getTargetFilename();
                break;
            default:
                filename = author.getTargetVolumeName(volNum);

        }
        return filename;
    }

    /**
     * Get the path to a text file
     * eg res/source/fer4.txt
     *
     * @param author the author of the file
     * @param volNum the volume number of the file
     * @return the path to the text file
     */
    public static String getTextFilePath(Author author, int volNum, String fileSeparator) {
        return getSourceFolder(author, fileSeparator) + fileSeparator + getTextFileName(author, volNum);
    }

    /**
     * Get the path to the source folder of an author, relative to the res directory
     * eg target/fer
     *
     * @param author
     * @return
     */
    public static String getSourceFolder(Author author, String fileSeparator) {
        return "source" + fileSeparator + author.getFolder();
    }

    /**
     * Get the name of a text file
     * eg fer4.txt
     *
     * @param author the author of the file
     * @param volNum the volume number of the file
     * @return the name of the text file
     */
    public static String getTextFileName(Author author, int volNum) {
        String filename;

        switch (author) {
            case BIBLE:
                filename = BibleBook.values()[volNum - 1].getSourceFilename();
                break;
            case HYMNS:
                filename = HymnBook.values()[volNum - 1].getSourceFilename();
                break;
            default:
                filename = author.getSourceVolumeName(volNum);

        }
        return filename;
    }

    /**
     * Get the path to the index folder, relative to the res directory
     *
     * @param author
     * @return
     */
    public static String getIndexFilePath(Author author, String fileSeparator) {
        return getTargetFolder(author, fileSeparator) + fileSeparator + "index-" + author.getCode().toLowerCase() + ".idx";
    }

    /**
     * Get the index file name
     *
     * @param author
     * @return
     */
    public static String getIndexFileName(Author author) {
        return "index-" + author.getCode().toLowerCase() + ".idx";
    }

    /**
     * Get the name of an author's contents page
     *
     * @param author
     * @return
     */
    public static String getContentsFileName(Author author) {
        return author.getCode().toLowerCase() + "-contents.html";
    }

    /**
     * Get the path to an author's contents page, relative to res directory
     *
     * @param author
     * @return
     */
    public static String getContentsFilePath(Author author, String fileSeparator) {
        return getTargetFolder(author, fileSeparator) + fileSeparator + author.getCode().toLowerCase() + "-contents.html";
    }

}
