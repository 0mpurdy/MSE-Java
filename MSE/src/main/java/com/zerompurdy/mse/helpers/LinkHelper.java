package com.zerompurdy.mse.helpers;

import com.zerompurdy.mse.data.author.Author;

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
     * If it's not the first section, also add the section number
     * eg ../../target/jnd/jnd30.html#8:18
     *
     * @param author author of the result
     * @param volume volume the result is in
     * @param page page the result is on
     * @param section section the result is in
     * @return link to the referenced result
     */
    public static String getMinistryHtmlLink(Author author, int volume, int page, int section) {
        if ((section) > 0) {
            return "../../" + FileHelper.getHtmlFile(author, volume, "/") + "#" + page +  ":" + section;
        }

        return "../../" + FileHelper.getHtmlFile(author, volume, "/") + "#" + page;
    }

}
