package com.lake_team.fistserios.controller.rest;

import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.model.NewsSourceType;
import com.lake_team.fistserios.service.NewsApiService;
import com.lake_team.fistserios.service.RedditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
@CrossOrigin
public class NewsController {

    private final NewsApiService newsApiService;
    private final RedditService  redditService;

    /**
     * GET /news?page=0&size=10&category=Technology&source=GUARDIAN
     * Усі параметри крім page/size — необов'язкові.
     */
    @GetMapping
    public Page<NewsItem> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String search) {

        NewsSourceType sourceType = null;
        if (source != null && !source.isBlank()) {
            try {
                sourceType = NewsSourceType.valueOf(source.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return newsApiService.getNewsPage(page, size, category, sourceType, search);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsItem> getById(@PathVariable String id) {
        return newsApiService.getById(id);
    }

    /** Список доступних категорій (для фільтрів у UI). */
    @GetMapping("/categories")
    public List<String> getCategories() {
        return newsApiService.getAvailableCategories();
    }

    /**
     * Запускає асинхронне оновлення з усіх джерел.
     * Повертає 202 Accepted одразу, не чекаючи завершення.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh() {
        newsApiService.refreshAsync();
        return ResponseEntity.accepted().build();
    }

    /** Завантажити новини з Reddit (all subreddits) */
    @PostMapping("/refresh/reddit")
    public ResponseEntity<String> refreshReddit() {
        List<NewsItem> saved = redditService.fetchAndSaveAll();
        return ResponseEntity.ok("Saved " + saved.size() + " Reddit articles");
    }
}
