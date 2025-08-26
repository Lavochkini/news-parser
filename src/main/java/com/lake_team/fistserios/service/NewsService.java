package com.lake_team.fistserios.service;/*
  @author Bogdan
  @project fistserios
  @class NewsService
  @version 1.0.0
  @since 26.08.2025 - 16.40
*/

import org.springframework.stereotype.Service;

@Service
public class NewsService {
    public String fetchNews() {
        return "Сьогодні чудовий день для навчання Spring + JavaFX!";
    }
}
