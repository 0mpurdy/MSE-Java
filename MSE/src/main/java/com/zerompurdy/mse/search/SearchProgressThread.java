package com.zerompurdy.mse.search;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Michael on 17/11/2015.
 */
public class SearchProgressThread extends Thread {

    AtomicInteger progress;
    int numAuthors;

    ProgressBar progressBar;
    Label progressLabel;

    public SearchProgressThread(ProgressBar progressBar, Label progressLabel, AtomicInteger progress, int numAuthors) {
        this.progressBar = progressBar;
        this.progressLabel = progressLabel;
        this.progress = progress;
        this.numAuthors = numAuthors;
    }

    @Override
    public void run() {
        while (progress.get() < (1000 * numAuthors)) {
            Platform.runLater(() -> progressBar.setProgress(((double) progress.get() / numAuthors) / 1000));
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        progressBar.setProgress(1.0);
    }
}
