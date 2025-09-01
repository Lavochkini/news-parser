package com.lake_team.fistserios.gui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;



@Component

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        System.out.println("Login attempt: " + username + " / " + password);

        // TODO: тут пізніше зробимо перевірку через базу (UserService)
    }

    @FXML
    private void openRegistration() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/registration.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) usernameField.getScene().getWindow(); // поточне вікно

            double width = stage.getScene().getWidth();
            double height = stage.getScene().getHeight();

            boolean maximized = stage.isMaximized();

            Scene scene = new Scene(root, width, height);
            stage.setScene(scene);

            stage.setMaximized(maximized);

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void openGuest() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/main.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) usernameField.getScene().getWindow();

            double width = stage.getScene().getWidth();
            double height = stage.getScene().getHeight();

            boolean maximized = stage.isMaximized();
            Scene scene = new Scene(root, width, height);
            stage.setScene(scene);

            stage.setMaximized(maximized);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}


