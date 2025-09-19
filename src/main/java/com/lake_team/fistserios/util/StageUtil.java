package com.lake_team.fistserios.util;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class StageUtil {

    // універсальний метод для переключення сцени і збереження стану
    public static void switchScene(Stage stage, Scene newScene) {
        // зберігаємо попередній стан
        double width = stage.getWidth();
        double height = stage.getHeight();
        boolean maximized = stage.isMaximized();

        stage.setScene(newScene);

        if (maximized) {
            stage.setMaximized(true); // відновлюємо фулскрін
        } else {
            stage.setWidth(width);
            stage.setHeight(height);
        }
        stage.show();
    }
}
