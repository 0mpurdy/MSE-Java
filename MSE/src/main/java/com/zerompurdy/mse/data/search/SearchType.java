package mse.data.search;

/**
 * @author Michael Purdy
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

    public boolean search(String[] currentLineTokens, String[] searchTokens) {
        boolean valid = false;

        switch (this) {
            case MATCH:
                valid = clauseSearch(currentLineTokens, searchTokens);
                break;
            case PHRASE:
                valid = scopeWordsInOrder(currentLineTokens, searchTokens);
                break;
            case SENTENCE:
                valid = wordSearch(currentLineTokens, searchTokens);
                break;
            case PARAGRAPH:
                valid = wordSearch(currentLineTokens, searchTokens);
        }

        return valid;
    }

    public static boolean wildSearch(String[] currentLineTokens, String[] searchTokens) {
        // if any of the line tokens match the search tokens return true else return false

        for (String nextSearchToken : searchTokens) {
            for (String nextLineToken : currentLineTokens) {
                if (nextSearchToken.equals(nextLineToken)) {
                    return true;
                }
            }
        }

        // if it reaches this point as a wild search then no tokens were found
        return false;
    }

    private boolean wordSearch(String[] currentLineTokens, String[] searchTokens) {

        for (String nextSearchToken : searchTokens) {
            boolean foundCurrentSearchToken = false;
            for (String nextLineToken : currentLineTokens) {
                if (nextSearchToken.equals(nextLineToken)) {
                    foundCurrentSearchToken = true;
                }
            }
            if (!foundCurrentSearchToken) return false;
        }

        // if it reaches this point all tokens were found
        return true;
    }

    private boolean clauseSearch(String[] currentLineTokens, String[] searchTokens) {

        // read through the clause finding each word in order
        // return false if reached the end without finding every word

        // true if the next word should be next word in the search tokens
        boolean currentWordIsSearchToken;

        // position of the next token to find in the search tokens array
        int j = 0;

        //
        for (int i = 0; i < currentLineTokens.length; i++) {

            if (currentLineTokens[i].equals("")) continue;

            currentWordIsSearchToken = false;
            if (currentLineTokens[i].equalsIgnoreCase(searchTokens[j])) {
                j++;
                currentWordIsSearchToken = true;
            }

            if (j > 0) {

                // if all words found in order return true
                if (j == searchTokens.length) return true;

                // if current word wasn't a search token reset j
                if (!currentWordIsSearchToken) j = 0;
            }
        }

        return false;

    }

    private boolean scopeWordsInOrder(String[] lineTokens, String[] searchTokens) {
        int indexNextLineToken = 0;
        int indexNextSearchToken = 0;

        while (indexNextLineToken < lineTokens.length) {
            if (lineTokens[indexNextLineToken].toUpperCase().equals(searchTokens[indexNextSearchToken])) {
                indexNextSearchToken++;

                // if no more tokens to find
                if (indexNextSearchToken == searchTokens.length) return true;
            }
            indexNextLineToken++;
        }

        // if gone through all the line and not found all the tokens in the order
        return false;
    }

}
