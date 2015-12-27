package mse.data;

import mse.common.ILogger;
import mse.common.LogLevel;
import mse.common.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mj_pu_000 on 09/09/2015.
 */
public class AuthorIndex {

    private ILogger logger;

    private Author author;
    private HashMap<String, Integer> tokenCountMap;
    private HashMap<String, short[]> references;

    public AuthorIndex(Author author, ILogger logger) {
        this.author = author;
        tokenCountMap = new HashMap<>();
        references = new HashMap<>();
        this.logger = logger;
    }

    public Author getAuthor() {
        return author;
    }

    public String getAuthorName() {
        return author.getName();
    }

    public HashMap<String, Integer> getTokenCountMap() {
        return tokenCountMap;
    }

    public int getTokenCount(String token) {
        if (tokenCountMap.get(token) != null) {
            return tokenCountMap.get(token);
        } else {
            return 0;
        }
    }

    public short[] getReferences(String key) {
        return references.get(key);
    }

    public void loadIndex(String resLocation) {

        // try to load the index of the current author
        try {
            InputStream inStream = new FileInputStream(resLocation + author.getIndexFilePath());
            BufferedInputStream bInStream = new BufferedInputStream(inStream);
            ObjectInput input = new ObjectInputStream(bInStream);
            this.tokenCountMap = (HashMap<String, Integer>) input.readObject();
            this.references = (HashMap<String, short[]>) input.readObject();
        } catch (FileNotFoundException fnfe) {
            logger.log(LogLevel.HIGH, "Could not file find file: " + resLocation + author.getIndexFilePath());
        } catch (IOException ioe) {
            logger.log(LogLevel.HIGH, "Error loading from: " + author.getIndexFilePath());
        } catch (ClassCastException cce) {
            logger.log(LogLevel.HIGH, "Error casting class when loading new index");
        } catch (ClassNotFoundException cnfe) {
            logger.log(LogLevel.HIGH, "Class not found when loading new index");
        }

    }

}
