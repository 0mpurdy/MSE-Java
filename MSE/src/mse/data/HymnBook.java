/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse.data;

/**
 *
 * @author mj_pu_000
 */
public enum HymnBook {

    h1973("1973 Hymn Book", "hymns1973.txt", "hymns1973.htm"),
    h1962("1962 Hymn Book", "hymns1962.txt", "hymns1962.htm"),
    h1951("1951 Hymn Book", "hymns1951.txt", "hymns1951.htm"),
    h1932("1932 Hymn Book", "hymns1932.txt", "hymns1932.htm"),
    h1903("1903 Hymn Book", "hymns1903.txt", "hymns1903.htm");

    private String name;
    private String inputFilename;
    private String outputFilename;

    HymnBook(String name, String inputFilename, String outputFilename) {
        this.name = name;
        this.inputFilename = inputFilename;
        this.outputFilename = outputFilename;
    }

    public String getInputFilename() {
        return inputFilename;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOutputFilename() {
        return outputFilename;
    }
}
