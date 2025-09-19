package com.lake_team.fistserios.gui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;
import javafx.scene.control.Label;
import javafx.scene.control.Button;




import java.io.IOException;

@Component
public class MainController {

    @FXML
    private VBox rightVBox;

    @FXML
    private Button settingsButton;

    @FXML
    private TextField topicsearchField;

    @FXML
    private TextField mainsearchField;

    private VBox settingsMenu;

    @FXML
    private void toggleSettingsMenu() {
        boolean isVisible = rightVBox.isVisible();
        rightVBox.setVisible(!isVisible);
        rightVBox.setManaged(!isVisible); // важливо для layout
    }

    @FXML
    private void openLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) mainsearchField.getScene().getWindow();

        double width = stage.getScene().getWidth();
        double height = stage.getScene().getHeight();
        boolean maximized = stage.isMaximized();

        stage.setScene(new Scene(root, width, height));
        stage.setMaximized(maximized);
        stage.show();
    }

    @FXML
    private void openRegistration() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/registration.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) mainsearchField.getScene().getWindow();

        double width = stage.getScene().getWidth();
        double height = stage.getScene().getHeight();
        boolean maximized = stage.isMaximized();

        stage.setScene(new Scene(root, width, height));
        stage.setMaximized(maximized);
        stage.show();
    }
}
