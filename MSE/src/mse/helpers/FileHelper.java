package mse.helpers;

import mse.data.author.Author;
import mse.data.author.BibleBook;
import mse.data.author.HymnBook;

/**
 * @author Michael Purdy
 *         Helps to generate links and folder paths
 */
public abstract class FileHelper {

    /**
     * Get the path to an html file
     * eg res/target/fer/fer4.html
     *
     * @param author author of the file
     * @param volNum volume number of the file
     * @return the name of the file
     */
    public static String getHtmlFilePath(Author author, int volNum) {
        String filename;

        switch (author) {
            case BIBLE:
                filename = author.getTargetPath(BibleBook.values()[volNum - 1].getTargetFilename());
                break;
            case HYMNS:
                filename = author.getTargetPath(HymnBook.values()[volNum - 1].getTargetFilename());
                break;
            default:
                filename = author.getTargetVolumePath(volNum);

        }
        return filename;
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
     * eg ../MSE-Res-Lite/res/source/fer4.txt
     *
     * @param author the author of the file
     * @param volNum the volume number of the file
     * @return the path to the text file
     */
    public static String getTextFilePath(Author author, int volNum) {
        String filename;

        switch (author) {
            case BIBLE:
                filename = author.getSourcePath(BibleBook.values()[volNum - 1].getSourceFilename());
                break;
            case HYMNS:
                filename = author.getSourcePath(HymnBook.values()[volNum - 1].getSourceFilename());
                break;
            default:
                filename = author.getSourceVolumePath(volNum);

        }
        return filename;
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

}
