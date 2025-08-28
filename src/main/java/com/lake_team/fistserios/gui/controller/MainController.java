package com.lake_team.fistserios.gui.controller;/*
  @author Bogdan
  @project fistserios
  @class MainController
  @version 1.0.0
  @since 26.08.2025 - 16.39
*/

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import org.springframework.stereotype.Component;
import com.lake_team.fistserios.service.NewsService;

@Component // Щоб Spring керував цим контролером
public class MainController {

    private final NewsService newsService;

    public MainController(NewsService newsService) {
        this.newsService = newsService;
    }

    @FXML
    private void loadNews() {
        String news = newsService.fetchNews();
        Alert alert = new Alert(Alert.AlertType.INFORMATION, news);
        alert.setTitle("Новини");
        alert.setHeaderText("Остання новина");
        alert.showAndWait();
    }
}