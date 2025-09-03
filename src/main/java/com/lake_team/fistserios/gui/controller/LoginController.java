package com.lake_team.fistserios.gui.controller;

import com.lake_team.fistserios.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javafx.scene.control.Alert;
import com.lake_team.fistserios.controller.AuthController;


@Component

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private void handleLogin() {
        String email = usernameField.getText();
        String password = passwordField.getText();

        try {
            // Формуємо JSON
            String jsonBody = String.format("{\"email\":\"%s\", \"password\":\"%s\"}", email, password);

            // Створюємо HTTP клієнт
            HttpClient client = HttpClient.newHttpClient();

            // Готуємо запит
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/auth/login")) // твій бекенд
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // Відправляємо
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Виводимо результат
            System.out.println("Response code: " + response.statusCode());
            System.out.println("Response body: " + response.body());


            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                String username = json.getString("username");

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Успішний вхід");
                alert.setHeaderText(null);
                alert.setContentText("Ви успішно увійшли в систему як " + username);
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Помилка входу");
                alert.setHeaderText(null);
                alert.setContentText("Невірний email або пароль");
                alert.showAndWait();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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