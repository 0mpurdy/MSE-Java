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

    public static String getHtmlFileName(Config cfg, Author author, int volNum) {
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

    public static String getTextFileName(Config cfg, Author author, int volNum) {
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

}
