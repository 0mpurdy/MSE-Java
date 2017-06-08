package mse.search;

import mse.common.config.Config;
import mse.common.log.ILogger;
import mse.common.log.LogLevel;
import mse.data.author.Author;
import mse.data.author.AuthorIndex;
import mse.helpers.FileHelper;

import java.io.File;
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
            authorIndex.loadIndex(cfg.getResDir());
            authorIndexes.put(author.getCode(), authorIndex);
            logger.log(LogLevel.TRACE, "\tLoading Index: " + authorIndex.getAuthorName() + " from: " + cfg.getResDir() + FileHelper.getIndexFilePath(authorIndex.getAuthor(), File.separator));
            logger.log(LogLevel.TRACE, "\tIndex count: " + authorIndex.getTokenCountMap().keySet().size());
        }

        return authorIndex;
    }

}
