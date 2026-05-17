package com.lake_team.fistserios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.model.NewsSourceType;
import com.lake_team.fistserios.model.gnews.GNewsArticle;
import com.lake_team.fistserios.model.gnews.GNewsResponse;
import com.lake_team.fistserios.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GNewsService {

    private final NewsRepository newsRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gnews.api.key}")
    private String apiKey;

    @Value("${gnews.api.url}")
    private String baseUrl;

    public List<NewsItem> fetchAndSaveTopHeadlines(String lang) {
        try {
            String url = baseUrl + "?lang=" + lang + "&max=50&token=" + apiKey;
            log.info("Fetching news from GNews API");
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                log.warn("Empty response from GNews API");
                return Collections.emptyList();
            }
            GNewsResponse gNewsResponse = objectMapper.readValue(response, GNewsResponse.class);
            if (gNewsResponse.getArticles() == null) {
                return Collections.emptyList();
            }
            return gNewsResponse.getArticles().stream()
                    .map(this::mapToNewsItem)
                    .map(this::saveIfNotExists)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching from GNews API: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private NewsItem mapToNewsItem(GNewsArticle article) {
        return NewsItem.builder()
                .title(article.getTitle())
                .description(article.getDescription())
                .fullContent(article.getContent())
                .url(article.getUrl())
                .imageUrl(article.getImage())
                .publishedAt(parseDate(article.getPublishedAt()))
                .source(article.getSource() != null ? article.getSource().getName() : "GNews")
                .sourceType(NewsSourceType.GNEWS)
                .build();
    }

    private NewsItem saveIfNotExists(NewsItem item) {
        return newsRepository.findByUrl(item.getUrl())
                .orElseGet(() -> newsRepository.save(item));
    }

    private LocalDateTime parseDate(String date) {
        try {
            return LocalDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.warn("Cannot parse GNews date: {}", date);
            return LocalDateTime.now();
        }
    }
}
