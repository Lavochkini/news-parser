package com.lake_team.fistserios.gui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent;

import java.io.IOException;

public class RegistrationController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField emailField;

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String email = emailField.getText();

        System.out.println("Реєстрація: " + username + " / " + password + " / " + email);

        // TODO: тут буде логіка збереження користувача в базу
    }

    @FXML
    private void openLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) usernameField.getScene().getWindow(); // поточне вікно
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openGuest(MouseEvent event) {
        System.out.println("Гість натиснув!");
    }


    @FXML
    private void backToLogin() {
        System.out.println("Back to login clicked!");
    }
}

