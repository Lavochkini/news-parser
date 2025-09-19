package com.lake_team.fistserios.gui.controller;

import com.lake_team.fistserios.util.StageUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class LoginController {
    @FXML private ImageView imageView;
    @FXML private ImageView imageViewRight;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label emailErrorLabel;
    @FXML private Label passwordErrorLabel;

    @FXML
    public void initialize() {
        Image leftImage = new Image(getClass().getResource("/gui/images/left_art.png").toExternalForm());
        imageView.setImage(leftImage);

        Image rightImage = new Image(getClass().getResource("/gui/images/right_art.png").toExternalForm());
        imageViewRight.setImage(rightImage);
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();
        boolean hasError = false;

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
        } else {
            passwordErrorLabel.setText("");
        }

        if (hasError) return;

        try {
            HttpClient client = HttpClient.newHttpClient();
            String jsonBody = String.format("{\"email\":\"%s\", \"password\":\"%s\"}", email, password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/main.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) emailField.getScene().getWindow();
                Scene scene = new Scene(root);

                StageUtil.switchScene(stage, scene);
            } else {
                passwordErrorLabel.setText("Invalid email or password");
            }
        } catch (Exception e) {
            e.printStackTrace();
            passwordErrorLabel.setText("Server error. Try again later.");
        }
    }

    @FXML
    private void openRegistration() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/registration.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) emailField.getScene().getWindow();
        Scene scene = new Scene(root);
        StageUtil.switchScene(stage, scene);
    }

    @FXML
    private void openGuest() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/main.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) emailField.getScene().getWindow();
        Scene scene = new Scene(root);
        StageUtil.switchScene(stage, scene);
    }
}
