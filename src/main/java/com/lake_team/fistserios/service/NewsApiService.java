package com.lake_team.fistserios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.model.NewsSourceType;
import com.lake_team.fistserios.model.news_api.NewsApiArticle;
import com.lake_team.fistserios.model.news_api.NewsApiResponse;
import com.lake_team.fistserios.repository.NewsRepository;
import com.lake_team.fistserios.repository.NewsSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

    @Cacheable(value = "newsPage", key = "#page + '-' + #size + '-' + #category + '-' + #sourceType")
    public Page<NewsItem> getNewsPage(int page, int size, String category, NewsSourceType sourceType) {
        Specification<NewsItem> spec = Specification
                .where(NewsSpecification.hasCategory(category))
                .and(NewsSpecification.hasSourceType(sourceType));
        return newsRepository.findAll(spec, PageRequest.of(page, size, Sort.by("publishedAt").descending()));
    }

    @Cacheable(value = "newsById", key = "#id")
    public ResponseEntity<NewsItem> getById(Long id) {
        return newsRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public List<NewsItem> getAllNews() {
        return newsRepository.findAll();
    }

    public List<String> getAvailableCategories() {
        return newsRepository.findDistinctCategories();
    }

    @CacheEvict(value = {"newsPage", "newsById"}, allEntries = true)
    @Async
    public CompletableFuture<Void> refreshAsync() {
        return CompletableFuture.runAsync(() -> fetchAndSaveTopHeadlines("us"));
    }

    public List<NewsItem> fetchAndSaveTopHeadlines(String country) {
        try {
            String url = baseUrl + "?country=" + country + "&pageSize=50&apiKey=" + apiKey;
            log.info("Fetching news from NewsAPI");
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                log.warn("Empty response from NewsAPI");
                return Collections.emptyList();
            }
            NewsApiResponse newsApiResponse = objectMapper.readValue(response, NewsApiResponse.class);
            return newsApiResponse.getArticles().stream()
                    .map(this::mapToNewsItem)
                    .map(this::saveIfNotExists)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error while fetching news from NewsAPI: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private NewsItem mapToNewsItem(NewsApiArticle article) {
        return NewsItem.builder()
                .title(article.getTitle())
                .description(article.getDescription())
                .fullContent(article.getContent())
                .url(article.getUrl())
                .imageUrl(article.getUrlToImage())
                .publishedAt(parseDate(article.getPublishedAt()))
                .source(article.getSource() != null ? article.getSource().getName() : "NewsAPI")
                .sourceType(NewsSourceType.NEWS_API)
                .build();
    }

    private NewsItem saveIfNotExists(NewsItem item) {
        return newsRepository.findByUrl(item.getUrl())
                .orElseGet(() -> newsRepository.save(item));
    }

    private LocalDateTime parseDate(String publishedAt) {
        try {
            return LocalDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.warn("Cannot parse date: {}", publishedAt);
            return LocalDateTime.now();
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void scheduledNewsUpdate() {
        log.info("Running scheduled news update...");
        fetchAndSaveTopHeadlines("us");
    }
}
