/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse;

import mse.Author;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 *
 * @author michael
 */
public class FXMLSearchController implements Initializable {

    private HtmlBuilder htmlB = new HtmlBuilder();
    private Config cfg;
    private OpenBookHandler openBookHandler;

    @FXML
    Button refineButton;
    @FXML
    Menu booksMenu;
    @FXML
    TextField searchBox;
    @FXML
    GridPane checkBoxPane;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // try to recover config options
        if ((cfg = readConfig()) == null) {
            cfg = new Config();
            cfg.save();
        }

        cfg.setSetup(true);
        
        // TODO check if they're available first
        int i = 0;
        for (Author nextAuthor : Author.values()) {
            if (nextAuthor.isSearchable()) {
                CheckBox nextCheckBox = new CheckBox(nextAuthor.getCode());
                nextCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
                    public void changed(ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) {
                        if (!cfg.isSettingUp()) {
                            cfg.setSelectedAuthor(nextAuthor, true);
                            cfg.save();
                        }
                    }
                });

                // select if it is to be searched or not
                nextCheckBox.setSelected(cfg.getSelectedAuthor(nextAuthor));

                checkBoxPane.add(nextCheckBox, i % 6, i / 6);
                i++;
            }
            MenuItem nextMenuItem = new MenuItem(nextAuthor.getName());
            nextMenuItem.setOnAction(new OpenBookHandler(cfg, htmlB));
            booksMenu.getItems().add(nextMenuItem);
            if ((booksMenu.getItems().size() == 2) || (booksMenu.getItems().size() == Author.values().length - 2)) {
                booksMenu.getItems().add(new SeparatorMenuItem());
            }
        }
        
        cfg.setSetup(false);

        // initialise the search box
        searchBox.setText(cfg.getSearchString());

    }

    @FXML
    public void handlesSearch(ActionEvent e) {

        // save new config
        cfg.setSearchString(searchBox.getText());
        cfg.save();

        if ((searchBox.getText().contains("*")) && (searchBox.getText().contains(" "))) {
            // TODO fix alerts
//            new Alert("Error", "Wildcard searches must only have one word");
        } else if (searchBox.getText().equals("")) {
//            new Alert("Error", "Invalid search string");
        } else {
            // if any authors are selected            
            if (cfg.isAuthorSelected()) {
                // TODO progress window
//                Progress progressSearch = new Progress("Searching ...", Constants.PROGRESS_BAR_MAX);
//                Thread t = new Thread(new VolumeSearch(this, progressSearch));
//                enableButtons(false);
//                t.start();
            } else {
                // TODO error
//                new Alert("Error", "At least one checkbox must be ticked");
            }
        }

    }

    @FXML
    public void handlesRefine(ActionEvent e) {
    }

    private void updateSelectedAuthor(ItemEvent itemEvent, Author author) {
        cfg.setSelectedAuthor(author, true);
    }

    private Config readConfig() {
        try {
            Gson gson = new Gson();
            FileReader fr = new FileReader("Config.txt");
            Path jsonPath = Paths.get("Config.txt");
            String json = new String(Files.readAllBytes(jsonPath));
            System.out.println("Config loaded.");
            return gson.fromJson(json, Config.class);
        } catch (IOException fnfe) {
            System.out.println("Config file could not be read.");
            return null;
        }
    }

}
