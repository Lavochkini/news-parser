package com.lake_team.fistserios;/*
  @author Bogdan
  @project fistserios
  @class JavaFxApp
  @version 1.0.0
  @since 26.08.2025 - 15.37
*/
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import javafx.scene.Parent;

public class JavaFxApp extends Application {
    private static String[] savedArgs;
    private ConfigurableApplicationContext springContext;

    public static void launchApp(String[] args) {
        savedArgs = args;
        launch(args); // Запускає JavaFX Application
    }

    @Override
    public void init() {
        // Піднімаємо Spring Boot контекст
        springContext = new SpringApplicationBuilder(NewsParserApplication.class)
                .run(savedArgs);
    }

    @Override
    public void start(Stage stage) throws Exception {
        // Завантажуємо FXML через Spring
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/gui/login.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);

        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 900, 650);  // трохи більше за дефолт
        stage.setScene(scene);

        stage.setTitle("News Parser");
        stage.show();
    }

    @Override
    public void stop() {
        springContext.close(); // Закриваємо Spring при виході
    }
}
