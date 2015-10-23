/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mse;

import mse.common.Author;
import mse.common.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;


/**
 *
 * @author michael
 */

public class Index {

    public static final int NOT_LOADED = 0;
    public static final int LOADED = 1;
    public static final int RELOADED = 2;
    
    public static final int WORD_NOT_FOUND = -1;

    private Config cfg;

    // maps to show which authors are loaded or not
    private HashMap<Author, Boolean> indexAvailable;
    private HashMap<Author, Boolean> indexLoaded;

    private HashMap<String, Integer> wordsMap; //generated from saWords, key is word id
    private HashMap<String, Long> offsetsMap; //generated from laOffsets, key from same index in saVolPages
    private HashMap<String, Long> lengthsMap; //generated from laLengths, key from same index in saVolPages
    private ArrayList<String> occursList; //List of occurrences "strings" and stored in index file

    private String[] wordsList; //List of words indexed by word id and stored in index file
    private String[] volAndPageList; //List of vol/page numbers and stored in index file
    private long[] offsetsList; //List of page offsets in html file (same index as saVolPages) and stored in index file
    private long[] lengthsList; //List of page lengths in html file (same index as saVolPages) and stored in index file

    HashMap<Author, String[]> authorWordsMap = new HashMap<>(); //cache of wordsList for each author
    HashMap<Author, HashMap<String, Integer>> authorWordsMap_Map = new HashMap<>(); //cache of wordsMap for each author
    HashMap<Author, HashMap<String, Long>> authorOffsetsMap = new HashMap<>(); //cache of offsetsMap for each author
    HashMap<Author, HashMap<String, Long>> authorLengthsMap = new HashMap<>(); //cache of lengthsMap for each author
    HashMap<Author, ArrayList<String>> authorOccursMap = new HashMap<>(); //cache of occursList for each author

    public Index(Config cfg) {

        this.cfg = cfg;

        // check the availability of files
        for (Author nextAuthor : Author.values()) {
            if (nextAuthor.isSearchable()) {
                File nextFile = new File(cfg.getWorkingDir() + nextAuthor.getIndexFilePath());
                if (nextFile.exists()) {
                    indexAvailable.put(nextAuthor, Boolean.TRUE);
                } else {
                    indexAvailable.put(nextAuthor, Boolean.FALSE);
                }
            }
        }

        if (cfg.isAutoLoad()) {
            // if the files are to automatically be loaded

            // TODO 2 display progress
            try {

                // for each author if it can be searched - load it's index
                for (Author nextAuthor : Author.values()) {
                    loadAuthor(nextAuthor);
                    // TODO 2 update progress
                }
            } catch (Exception e) {
                System.out.println("Stopped index pre-load");
            }
            // TODO 2 close progress
        }
    }
    
    public boolean isAvailable(Author author) {
        return indexAvailable.get(author);
    }

    private void setDefaults() {

        //bible always is available, 
        for (Author nextAuthor : Author.values()) {
            if (nextAuthor.isSearchable()) {
                if (nextAuthor == Author.BIBLE) {
                    indexAvailable.put(nextAuthor, Boolean.TRUE);
                    indexLoaded.put(nextAuthor, Boolean.FALSE);
                } else {
                    indexAvailable.put(nextAuthor, Boolean.FALSE);
                    indexLoaded.put(nextAuthor, Boolean.FALSE);
                }
            }
        }

    }

    public int loadAuthor(Author author) {
        int result = NOT_LOADED;

        // if the author is not loaded
        if (!indexLoaded.get(author)) {
            File authorIndexFile = new File(cfg.getWorkingDir() + author.getIndexFilePath());
            if (authorIndexFile.exists()) {
                // if the index file exists

                // initialise
                wordsMap = new HashMap<>();
                offsetsMap = new HashMap<>();
                lengthsMap = new HashMap<>();
                occursList = new ArrayList<>();

                FileInputStream fisIndex = null;
                ObjectInputStream oisIndex = null;
                try {
                    fisIndex = new FileInputStream(authorIndexFile);
                    oisIndex = new ObjectInputStream(fisIndex);

                    wordsList = (String[]) oisIndex.readObject();
                    occursList = (ArrayList<String>) oisIndex.readObject();
                    volAndPageList = (String[]) oisIndex.readObject();
                    offsetsList = (long[]) oisIndex.readObject();
                    lengthsList = (long[]) oisIndex.readObject();

                    // clear the hashmaps and put map the values from the 
                    // corresponding list with a key
                    int i = 0;

                    wordsMap.clear();
                    for (String nextWord : wordsList) {
                        wordsMap.put(nextWord, i);
                        i++;
                    }

                    offsetsMap.clear();
                    i = 0;
                    // TODO -I what are the strings in the vol and pages list
                    for (String nextString : volAndPageList) {
                        offsetsMap.put(nextString, offsetsList[i]);
                        i++;
                    }

                    lengthsMap.clear();
                    i = 0;
                    // TODO -I what are the longs in the lengths list
                    for (Long nextLong : lengthsList) {
                        offsetsMap.put(volAndPageList[i], nextLong);
                    }

                    // add the values to the author map caches
                    authorWordsMap.put(author, wordsList);
                    authorWordsMap_Map.put(author, wordsMap);
                    authorOffsetsMap.put(author, offsetsMap);
                    authorLengthsMap.put(author, lengthsMap);
                    authorOccursMap.put(author, occursList);

                    // record the author as loaded
                    indexLoaded.put(author, Boolean.TRUE);
                    System.out.println("Loaded " + author.name() + " index (" + wordsList.length + " words)");
                    result = LOADED;

                } catch (Exception ex) {
                    System.out.println("Error reading index file: " + author.getIndexFilePath() + "\n" + ex);
                } finally {
                    // close the input streams
                    try {
                        if (fisIndex != null) {
                            fisIndex.close();
                        }
                        if (oisIndex != null) {
                            oisIndex.close();
                        }
                    } catch (IOException ex) {
                    }
                }

            } else {
                System.out.println("Could not load " + author.getName() + " index at: " + author.getIndexFilePath());
            }
        } else {
            wordsList = authorWordsMap.get(author);
            wordsMap = authorWordsMap_Map.get(author);
            offsetsMap = authorOffsetsMap.get(author);
            lengthsMap = authorLengthsMap.get(author);
            occursList = authorOccursMap.get(author);

            result = RELOADED;
        }
        return result;
    }
    
    public int getIndexPos(String strWord) {
        Integer integerIndex = wordsMap.get(strWord);
        if (integerIndex != null) {
            return integerIndex;
        } else {
            return WORD_NOT_FOUND;
        }
    }
    
    // TODO -I what does this do? ...
//    public byte[] getRefs(int intIndex) {
//        return (byte[])occursList.get(intIndex);
//    }

    public String[] getWordsList() {
        return wordsList;
    }
    
}
