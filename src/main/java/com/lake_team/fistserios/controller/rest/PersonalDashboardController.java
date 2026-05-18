package com.lake_team.fistserios.controller.rest;

import com.lake_team.fistserios.analysis.NewsAnalysis;
import com.lake_team.fistserios.analysis.NewsAnalysisRepository;
import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.repository.NewsRepository;
import com.lake_team.fistserios.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/user/dashboard")
@RequiredArgsConstructor
public class PersonalDashboardController {

    private final NewsAnalysisRepository analysisRepository;
    private final NewsRepository newsRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboard(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        // auth.getName() повертає email (з JWT), шукаємо User щоб отримати username
        com.lake_team.fistserios.model.User user = userRepository.findByEmail(auth.getName())
                .orElse(null);
        if (user == null) return ResponseEntity.status(401).build();
        String username = user.getUsername();

        // Статті які юзер аналізував вручну
        List<NewsAnalysis> analyses = analysisRepository
                .findAllByAnalyzedByUsernameOrderByAnalyzedAtDesc(username);

        List<Map<String, Object>> analyzedArticles = analyses.stream()
                .map(a -> {
                    Map<String, Object> dto = new LinkedHashMap<>();
                    dto.put("newsItemId",        a.getNewsItemId());
                    dto.put("credibilityScore",  a.getCredibilityScore());
                    dto.put("linguisticScore",   a.getLinguisticScore());
                    dto.put("crossSourceScore",  a.getCrossSourceScore());
                    dto.put("factCheckScore",    a.getFactCheckScore());
                    dto.put("sentimentLabel",    a.getSentimentLabel());
                    dto.put("readabilityLevel",  a.getReadabilityLevel());
                    dto.put("analyzedAt",        a.getAnalyzedAt());
                    // Збагачення даними статті
                    newsRepository.findById(a.getNewsItemId()).ifPresent(item -> {
                        dto.put("title",       item.getTitle());
                        dto.put("source",      item.getSource());
                        dto.put("url",         item.getUrl());
                        dto.put("imageUrl",    item.getImageUrl());
                        dto.put("publishedAt", item.getPublishedAt());
                        dto.put("sourceType",  item.getSourceType());
                    });
                    return dto;
                })
                .toList();

        // Улюблені статті
        Set<String> favoriteIds = user.getFavoriteNewsIds();

        List<Map<String, Object>> favoriteArticles = favoriteIds.stream()
                .map(id -> newsRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .map(item -> {
                    Map<String, Object> dto = new LinkedHashMap<>();
                    dto.put("id",          item.getId());
                    dto.put("title",       item.getTitle());
                    dto.put("source",      item.getSource());
                    dto.put("url",         item.getUrl());
                    dto.put("imageUrl",    item.getImageUrl());
                    dto.put("publishedAt", item.getPublishedAt());
                    dto.put("sourceType",  item.getSourceType());
                    dto.put("category",    item.getCategory());
                    // Додаємо оцінку якщо є
                    analysisRepository.findByNewsItemId(item.getId()).ifPresent(a ->
                            dto.put("credibilityScore", a.getCredibilityScore()));
                    return dto;
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("username",         username);
        response.put("totalAnalyzed",    analyzedArticles.size());
        response.put("totalFavorites",   favoriteArticles.size());
        response.put("analyzedArticles", analyzedArticles);
        response.put("favoriteArticles", favoriteArticles);

        return ResponseEntity.ok(response);
    }
}
