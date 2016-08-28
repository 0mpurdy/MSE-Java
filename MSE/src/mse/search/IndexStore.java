package mse.search;

import mse.common.config.Config;
import mse.common.log.ILogger;
import mse.common.log.LogLevel;
import mse.data.author.Author;
import mse.data.author.AuthorIndex;

import java.util.HashMap;

/**
 * @author Michael Purdy
 *  Store for caching author indexes
 */
public class IndexStore {

    Config cfg;

    HashMap<String, AuthorIndex> authorIndexes;

    public IndexStore(Config cfg) {
        this.cfg = cfg;
        authorIndexes = new HashMap<>();
    }

    public AuthorIndex getIndex(ILogger logger, Author author) {

        logger.log(LogLevel.TRACE, "\tLoading index for :" + author.getName());

        AuthorIndex authorIndex = authorIndexes.get(author.getCode());

        if (authorIndex == null) {
            authorIndex = new AuthorIndex(author, logger);
            authorIndex.loadIndex();
            authorIndexes.put(author.getCode(), authorIndex);
            logger.log(LogLevel.TRACE, "\tLoading Index: " + authorIndex.getAuthorName() + " from: " + authorIndex.getAuthor().getIndexFilePath());
            logger.log(LogLevel.TRACE, "\tIndex count: " + authorIndex.getTokenCountMap().keySet().size());
        }

        return authorIndex;
    }

}
