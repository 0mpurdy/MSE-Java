package mse.helpers;

import mse.data.author.Author;
import mse.search.AuthorSearchCache;

import java.io.IOException;

/**
 * @author Michael Purdy
 */
public interface IFileReader {

    String getFirstAuthorSectionHeader(AuthorSearchCache asc);

    int findNextAuthorPage(AuthorSearchCache asc);

    String getNextSectionHeader(AuthorSearchCache asc);

    String getNextAuthorSection(AuthorSearchCache asc);

    String[] getSearchTokens() throws IOException;

    void close();

    String readContentLine();

    String getNextResult(Author author) throws IOException;
}
