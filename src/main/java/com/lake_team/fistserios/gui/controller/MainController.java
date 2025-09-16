package com.lake_team.fistserios.gui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

@Component
public class MainController {
    @FXML
    private TextField topicsearchField;

    @FXML
    private TextField mainsearchField;

    @FXML
    private void openLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/login.fxml"));
        Parent root = loader.load();

        // get stage from any existing node in current scene
        Stage stage = (Stage) mainsearchField.getScene().getWindow();

        double width = stage.getScene().getWidth();
        double height = stage.getScene().getHeight();
        boolean maximized = stage.isMaximized();

        Scene scene = new Scene(root, width, height);
        stage.setScene(scene);
        stage.setMaximized(maximized);
        stage.show();
    }


    @FXML
    private void openRegistration() throws IOException{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/registration.fxml"));
        Parent root = loader.load();

        // get stage from any existing node in current scene
        Stage stage = (Stage) mainsearchField.getScene().getWindow();

        double width = stage.getScene().getWidth();
        double height = stage.getScene().getHeight();
        boolean maximized = stage.isMaximized();

        Scene scene = new Scene(root, width, height);
        stage.setScene(scene);
        stage.setMaximized(maximized);
        stage.show();
    }
}