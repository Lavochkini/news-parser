package com.lake_team.fistserios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.model.NewsSourceType;
import com.lake_team.fistserios.model.guardian.GuardianArticle;
import com.lake_team.fistserios.model.guardian.GuardianArticleFields;
import com.lake_team.fistserios.model.guardian.GuardianResponseWrapper;
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
public class GuardianService {

    private final NewsRepository newsRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${guardian.api.key}")
    private String apiKey;

    @Value("${guardian.api.url}")
    private String baseUrl;

    public List<NewsItem> fetchAndSaveNews(String section) {
        try {
            String sectionParam = (section != null && !section.isBlank()) ? "&section=" + section : "";
            String url = baseUrl + "?show-fields=thumbnail,trailText,bodyText&page-size=50&api-key=" + apiKey + sectionParam;
            log.info("Fetching news from Guardian API");
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                log.warn("Empty response from Guardian API");
                return Collections.emptyList();
            }
            GuardianResponseWrapper wrapper = objectMapper.readValue(response, GuardianResponseWrapper.class);
            if (wrapper.getResponse() == null || wrapper.getResponse().getResults() == null) {
                return Collections.emptyList();
            }
            return wrapper.getResponse().getResults().stream()
                    .map(this::mapToNewsItem)
                    .map(this::saveIfNotExists)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching from Guardian API: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private NewsItem mapToNewsItem(GuardianArticle article) {
        GuardianArticleFields fields = article.getFields();
        return NewsItem.builder()
                .title(article.getWebTitle())
                .description(fields != null ? fields.getTrailText() : null)
                .fullContent(fields != null ? fields.getBodyText() : null)
                .url(article.getWebUrl())
                .imageUrl(fields != null ? fields.getThumbnail() : null)
                .publishedAt(parseDate(article.getWebPublicationDate()))
                .source("The Guardian")
                .category(article.getSectionName())
                .sourceType(NewsSourceType.GUARDIAN)
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
            log.warn("Cannot parse Guardian date: {}", date);
            return LocalDateTime.now();
        }
    }
}
