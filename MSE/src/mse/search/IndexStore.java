package mse.search;

import mse.common.Config;
import mse.common.ILogger;
import mse.data.Author;
import mse.data.AuthorIndex;

import java.util.ArrayList;
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
