package com.lake_team.fistserios.service;

import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import com.lake_team.fistserios.model.news_api.NewsApiArticle;
import com.lake_team.fistserios.model.news_api.NewsApiResponse;


@Service
@RequiredArgsConstructor
@Slf4j
public class NewsApiService {

    private final NewsRepository newsRepository;

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    @Value("${news_api.key}")
    private String apiKey;

    @Value("${news_api.url}")
    private String baseUrl;

    public Page<NewsItem> getNewsPage(int page, int size) {
        return newsRepository.findAllByOrderByPublishedAtDesc(PageRequest.of(page, size));
    }

    public List<NewsItem> getAllNews() {
        return newsRepository.findAll();
    }

    public ResponseEntity<NewsItem> getById(Long id) {
        return newsRepository.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Async
    public CompletableFuture<Void> refreshAsync() {
        return CompletableFuture.runAsync(() -> fetchAndSaveTopHeadlines("us"));
    }

    public List<NewsItem> fetchAndSaveTopHeadlines(String country) {
        try {
            String url = baseUrl + "?country=" + country + "&pageSize=50&apiKey=" + apiKey; log.info("Fetching news from API: {}", url);
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                log.warn("Empty response from API");
                return Collections.emptyList(); }
            NewsApiResponse newsApiResponse = objectMapper.readValue(response, NewsApiResponse.class);
            return newsApiResponse.getArticles().stream() .map(this::mapToNewsItem) .map(this::saveIfNotExists) .collect(Collectors.toList());
        }
        catch (Exception e) {
            log.error("Error while fetching news: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private NewsItem mapToNewsItem(NewsApiArticle article) {
        return NewsItem.builder() .title(article.getTitle())
                .description(article.getDescription())
                .url(article.getUrl()) .imageUrl(article.getUrlToImage())
                .publishedAt(parseDate(article.getPublishedAt()))
                .source(article.getSource().getName()) .build();
    }

    private NewsItem saveIfNotExists(NewsItem item) {
        return newsRepository.findByUrl(item.getUrl()) .orElseGet(() -> newsRepository.save(item));
    }
    private LocalDateTime parseDate(String publishedAt) {
        try {
            return LocalDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
        }
        catch (Exception e) { log.warn("Cannot parse date: {}", publishedAt);
            return LocalDateTime.now();
        }
    }
    @Scheduled(cron = "0 0 * * * *")
    public void scheduledNewsUpdate() {
        log.info("Running scheduled news update...");
        fetchAndSaveTopHeadlines("us");
    }
}