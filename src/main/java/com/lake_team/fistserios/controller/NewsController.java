package com.lake_team.fistserios.controller;/*
  @author Bogdan
  @project fistserios
  @class NewsController
  @version 1.0.0
  @since 17.09.2025 - 18.26
*/

import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.repository.NewsRepository;
import com.lake_team.fistserios.service.NewsApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/news")
public class NewsController {

    private final NewsApiService newsApiService;

    private final NewsRepository newsRepository;

    @GetMapping("/all")
    public List<NewsItem> getAllNews() {
        return newsRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsItem> getNewsById(@PathVariable Long id) {
        return newsRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/manual-update")
    public List<NewsItem> manualUpdate() {
        return newsApiService.fetchAndSaveTopHeadlines("us");
    }
}