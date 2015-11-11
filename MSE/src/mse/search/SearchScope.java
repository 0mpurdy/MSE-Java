/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse.search;

/**
 *
 * @author michael
 */
public enum SearchScope {
    
    PHRASE("Phrase"), SENTENCE("Sentence contains words"), PARAGRAPH("Paragraph contains words"), PAGE("Page contains words");

    String menuName;

    SearchScope(String menuName) {
        this.menuName = menuName;
    }

    public String getMenuName() {
        return menuName;
    }
    
}
