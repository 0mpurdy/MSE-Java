/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse.search;

/**
 * @author michael
 */
public enum SearchType {

    MATCH("Exact Match"),
    PHRASE("Sentence contains words in order"),
    SENTENCE("Sentence contains words"),
    PARAGRAPH("Paragraph contains words");
//    PAGE("Page contains words");

    String menuName;

    SearchType(String menuName) {
        this.menuName = menuName;
    }

    public String getMenuName() {
        return menuName;
    }

    public static SearchType fromString(String text) {
        if (text != null) {
            for (SearchType nextSearchType : SearchType.values()) {
                if (text.equalsIgnoreCase(nextSearchType.menuName) || text.equalsIgnoreCase(nextSearchType.toString())) {
                    return nextSearchType;
                }
            }
        }
        return null;
    }

}
