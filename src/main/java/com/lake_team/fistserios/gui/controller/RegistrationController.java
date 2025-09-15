package com.lake_team.fistserios.gui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RegistrationController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField emailField;

    @FXML
    private Label userNameErrorLabel;

    @FXML
    private Label emailErrorLabel;

    @FXML
    private Label passwordErrorLabel;


    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String email = emailField.getText();

        boolean hasError = false;

        if (username == null || username.isEmpty()) {
            userNameErrorLabel.setText("Username is required");
            hasError = true;
        } else if (username.length() < 3) {
            userNameErrorLabel.setText("Username must be at least 3 characters");
            hasError = true;
        } else {
            userNameErrorLabel.setText(""); // no error
        }

        if (email == null || email.isEmpty()) {
            emailErrorLabel.setText("Write your E-mail");
            hasError = true;
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            emailErrorLabel.setText("Incorrect E-mail format");
            hasError = true;
        } else {
            emailErrorLabel.setText("");
        }

        if (password == null || password.isEmpty()) {
            passwordErrorLabel.setText("Password is required");
            hasError = true;
        }else if (password.length() < 8) {
            passwordErrorLabel.setText("Passwort must be at least 8 characters");
            hasError = true;
        }else {
            passwordErrorLabel.setText("");
        }

        if (hasError) {
            return; // stop registration if validation fails
        }

        try {
            // Формуємо JSON
            String jsonBody = String.format("{\"username\":\"%s\", \"email\":\"%s\", \"password\":\"%s\"}", username ,email, password);

            // Створюємо HTTP клієнт
            HttpClient client = HttpClient.newHttpClient();

            // Готуємо запит

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response code: " + response.statusCode());
            System.out.println("Response body: " + response.body());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/login.fxml"));
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

    @FXML
    private void openGuest() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/main.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) usernameField.getScene().getWindow(); // отримуємо поточне вікно
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
    private void backToLogin() {
        System.out.println("Back to login clicked!");
    }
}

