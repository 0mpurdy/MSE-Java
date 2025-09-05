package com.zerompurdy.mse;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MSE extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MSE.class.getResource("FXMLSearch.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 515, 340);
        stage.setTitle("MSE");
        stage.setScene(scene);
        stage.show();
    }
}