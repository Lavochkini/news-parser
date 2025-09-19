package com.lake_team.fistserios.gui.controller;

import com.lake_team.fistserios.util.StageUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;

public class MainController {
    @FXML private VBox rightVBox;
    @FXML private Button settingsButton;
    @FXML private TextField topicsearchField;
    @FXML private TextField mainsearchField;
    @FXML private BorderPane contentPane;

    @FXML
    private void toggleSettingsMenu() {
        boolean isVisible = rightVBox.isVisible();
        rightVBox.setVisible(!isVisible);
        rightVBox.setManaged(!isVisible);
    }

    @FXML
    private void openLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) mainsearchField.getScene().getWindow();
        Scene scene = new Scene(root);
        StageUtil.switchScene(stage, scene);
    }

    @FXML
    private void openRegistration() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/registration.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) mainsearchField.getScene().getWindow();
        Scene scene = new Scene(root);
        StageUtil.switchScene(stage, scene);
    }
}
