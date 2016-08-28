package mse.search;

import mse.common.config.Config;
import mse.common.log.ILogger;
import mse.data.author.Author;
import mse.data.author.AuthorIndex;

import java.util.HashMap;

/**
 * Created by mj_pu_000 on 10/11/2015.
 */
public class IndexStore {

    Config cfg;

    HashMap<String, AuthorIndex> authorIndexes;

    public IndexStore(Config cfg) {
        this.cfg = cfg;
        authorIndexes = new HashMap<>();
    }

    public AuthorIndex getIndex(ILogger logger, Author author) {

        AuthorIndex authorIndex = authorIndexes.get(author.getCode());

        if (authorIndex == null) {
            authorIndex = new AuthorIndex(author, logger);
            authorIndex.loadIndex(cfg.getResDir());
            authorIndexes.put(author.getCode(), authorIndex);
        }

        return authorIndex;
    }

}
