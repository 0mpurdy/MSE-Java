package mse.helpers;

import mse.data.author.Author;

/**
 * Created by mj_pu_000 on 07/06/2017.
 */
public class LinkHelper {

    /**
     * Get the Html link from a search result to the location it references
     * eg ../../target/fer/fer1.html#170
     *
     * @param author author of the result
     * @param volume volume the result is in
     * @param page page the result is on
     * @param section section the result is in
     * @return link to the referenced result
     */
    public static String getHtmlLink(Author author, int volume, int page, int section) {
        return getMinistryHtmlLink(author, volume, page, section);
    }

    /**
     * Get the Html link from a ministry search result to the location it references
     * eg ../../target/fer/fer1.html#170
     *
     * @param author author of the result
     * @param volume volume the result is in
     * @param page page the result is on
     * @param section section the result is in
     * @return link to the referenced result
     */
    public static String getMinistryHtmlLink(Author author, int volume, int page, int section) {
        // todo add section
//        if ((sentenceNum - 1) > 0) {
//            return author.getRelativeHtmlTargetPath(getFileName()) + "#" + pageNum + ":" + (sentenceNum - 1);
//        }

        return "../../" + FileHelper.getHtmlFile(author, volume, "/") + "#" + page;
    }

}
