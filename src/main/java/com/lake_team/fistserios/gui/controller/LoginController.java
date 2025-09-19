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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javafx.scene.control.Label;


@Component

public class LoginController {

    @FXML
    private TextField emailField;
    ;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label emailErrorLabel;

    @FXML
    private Label passwordErrorLabel;

    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        boolean hasError = false;

        // --- Email validation ---
        if (email == null || email.isEmpty()) {
            emailErrorLabel.setText("Write your E-mail");
            hasError = true;
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            emailErrorLabel.setText("Incorrect E-mail format");
            hasError = true;
        } else {
            emailErrorLabel.setText("");
        }

        // --- Password validation ---
        if (password == null || password.isEmpty()) {
            passwordErrorLabel.setText("Password is required");
            hasError = true;
        } else {
            passwordErrorLabel.setText("");
        }

        if (hasError) {
            return; // stop login if validation fails
        }

        try {
            // --- Make request to backend ---
            HttpClient client = HttpClient.newHttpClient();

            String jsonBody = String.format(
                    "{\"email\":\"%s\", \"password\":\"%s\"}", email, password
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // --- Check login result ---
            if (response.statusCode() == 200) {
                // Login successful → open main page
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/main.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) emailField.getScene().getWindow();

                double width = stage.getScene().getWidth();
                double height = stage.getScene().getHeight();
                boolean maximized = stage.isMaximized();

                Scene scene = new Scene(root, width, height);
                stage.setScene(scene);
                stage.setMaximized(maximized);
                stage.show();
            } else {
                // Instead of popup → inline error
                passwordErrorLabel.setText("Invalid email or password");
            }
        } catch (Exception e) {
            e.printStackTrace();
            passwordErrorLabel.setText("Server error. Try again later.");
        }
    }

    @FXML
    private void openRegistration() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/registration.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) emailField.getScene().getWindow(); // поточне вікно

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

            Stage stage = (Stage) emailField.getScene().getWindow();

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