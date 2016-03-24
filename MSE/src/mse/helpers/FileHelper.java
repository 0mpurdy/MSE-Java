package mse.helpers;

import mse.common.Config;
import mse.data.Author;
import mse.data.BibleBook;
import mse.data.HymnBook;

/**
 * Created by Michael Purdy on 23/12/2015.
 *
 * This helps get the file names of an html file
 */
public class FileHelper {

    public static String getHtmlFileName(Config cfg, Author author, int volNum) {
        String filename = cfg.getResDir();

        switch (author) {
            case BIBLE:
                filename += author.getTargetPath(BibleBook.values()[volNum - 1].getBookFileName());
                break;
            case HYMNS:
                filename += author.getTargetPath(HymnBook.values()[volNum - 1].getOutputFilename());
                break;
            default:
                filename += author.getVolumePath(volNum);

        }
        return filename;
    }

}
