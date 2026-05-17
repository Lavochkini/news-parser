package com.lake_team.fistserios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.model.NewsSourceType;
import com.lake_team.fistserios.model.news_api.NewsApiArticle;
import com.lake_team.fistserios.model.news_api.NewsApiResponse;
import com.lake_team.fistserios.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private final MongoTemplate mongoTemplate;

    @Value("${news_api.key}")
    private String apiKey;

    @Value("${news_api.url}")
    private String baseUrl;

    @Cacheable(value = "newsPage", key = "#page + '-' + #size + '-' + #category + '-' + #sourceType + '-' + #search")
    public Page<NewsItem> getNewsPage(int page, int size, String category, NewsSourceType sourceType, String search) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());

        List<Criteria> filters = new ArrayList<>();

        if (category != null && !category.isBlank()) {
            filters.add(Criteria.where("category").regex(category, "i"));
        }
        if (sourceType != null) {
            filters.add(Criteria.where("sourceType").is(sourceType));
        }
        if (search != null && !search.isBlank()) {
            filters.add(new Criteria().orOperator(
                    Criteria.where("title").regex(search, "i"),
                    Criteria.where("description").regex(search, "i"),
                    Criteria.where("fullContent").regex(search, "i")
            ));
        }

        Query query = new Query().with(pageable);
        if (!filters.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(filters));
        }

        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), NewsItem.class);
        List<NewsItem> items = mongoTemplate.find(query, NewsItem.class);
        return new PageImpl<>(items, pageable, total);
    }

    @Cacheable(value = "newsById", key = "#id")
    public ResponseEntity<NewsItem> getById(String id) {
        return newsRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public List<String> getAvailableCategories() {
        return mongoTemplate.findDistinct("category", NewsItem.class, String.class)
                .stream()
                .filter(c -> c != null && !c.isBlank())
                .sorted()
                .collect(Collectors.toList());
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
