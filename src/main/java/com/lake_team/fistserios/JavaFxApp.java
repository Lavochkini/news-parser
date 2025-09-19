package com.lake_team.fistserios;

import com.lake_team.fistserios.util.StageUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApp extends Application {
    private static String[] savedArgs;
    private ConfigurableApplicationContext springContext;

    public static void launchApp(String[] args) {
        savedArgs = args;
        launch(args);
    }

    @Override
    public void init() {
        springContext = new SpringApplicationBuilder(NewsParserApplication.class)
                .run(savedArgs);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/gui/login.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);

        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);

        // Тепер використовуємо switchScene, а не applyFullScreen
        StageUtil.switchScene(stage, scene);

        stage.setTitle("News Parser");
        stage.show();
    }

    @Override
    public void stop() {
        springContext.close();
    }
}
