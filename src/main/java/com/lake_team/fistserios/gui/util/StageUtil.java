package com.lake_team.fistserios.gui.util;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class StageUtil {

    public static void switchScene(Stage stage, Scene newScene) {
        // зберігаємо попередній стан
        double width = stage.getWidth();
        double height = stage.getHeight();
        boolean maximized = stage.isMaximized();

        stage.setScene(newScene);

        if (maximized) {
            stage.setMaximized(true);
        } else {
            stage.setWidth(width);
            stage.setHeight(height);
        }
        stage.show();
    }
}
