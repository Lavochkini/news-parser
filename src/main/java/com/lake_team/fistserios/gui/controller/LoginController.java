package com.lake_team.fistserios.gui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;


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
        System.out.println("Відкрити реєстрацію");
        // TODO: тут відкриватимемо нове вікно з registration.fxml
    }

    @FXML
    private void openGuest() {
        System.out.println("Увійти як гість");
        // TODO: відкриття гостьового інтерфейсу
    }
}
