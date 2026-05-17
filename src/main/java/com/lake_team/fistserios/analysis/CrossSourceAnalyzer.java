package com.lake_team.fistserios.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lake_team.fistserios.model.NewsItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Крос-перевірка через пошукові ендпоінти існуючих API.
 *
 * Витягує ключові слова із заголовку статті і шукає тему в
 * NewsAPI, Guardian та GNews. Кількість джерел що підтвердили
 * тему визначає score (0–35).
 *
 * Score:  0 джерел → 0 | 1 → 10 | 2 → 22 | 3 → 35
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CrossSourceAnalyzer {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${news_api.key}")
    private String newsApiKey;

    @Value("${guardian.api.key}")
    private String guardianKey;

    @Value("${gnews.api.key}")
    private String gnewsKey;

    private static final int[] SCORE_BY_SOURCES = {0, 10, 22, 35};

    // Слова що не несуть змістового навантаження для пошуку
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "up", "about", "into", "through", "after",
            "is", "are", "was", "were", "be", "been", "have", "has", "had",
            "do", "does", "did", "will", "would", "could", "should", "may", "might",
            "not", "no", "as", "it", "its", "this", "that", "he", "she", "they",
            "we", "you", "what", "how", "when", "where", "why", "than", "then",
            "says", "said", "over", "new", "more", "two", "one", "three"
    );

    public CrossSourceResult analyze(NewsItem item) {
        String keywords = extractKeywords(item.getTitle());

        if (keywords.isBlank()) {
            log.debug("No usable keywords from title: {}", item.getTitle());
            return CrossSourceResult.builder()
                    .score(0).sourcesConfirmed(0)
                    .confirmedSources(List.of()).keywordsUsed("").build();
        }

        String encoded = URLEncoder.encode(keywords, StandardCharsets.UTF_8);
        List<String> confirmed = new ArrayList<>();

        if (searchNewsApi(encoded))  confirmed.add("NewsAPI");
        if (searchGuardian(encoded)) confirmed.add("Guardian");
        if (searchGNews(encoded))    confirmed.add("GNews");

        int score = SCORE_BY_SOURCES[Math.min(confirmed.size(), 3)];

        log.debug("Cross-source '{}': {}/{} → score {}", keywords, confirmed.size(), 3, score);

        return CrossSourceResult.builder()
                .score(score)
                .sourcesConfirmed(confirmed.size())
                .confirmedSources(confirmed)
                .keywordsUsed(keywords)
                .build();
    }

    // ── пошук по кожному API ──────────────────────────────────────

    // Мінімальна кількість результатів щоб вважати тему "підтвердженою".
    // > 0 давало false positive для конспірацій (debunking articles теж рахувались).
    private static final int MIN_RESULTS = 3;

    private boolean searchNewsApi(String encodedQuery) {
        try {
            String url = "https://newsapi.org/v2/everything?q=" + encodedQuery
                    + "&pageSize=10&apiKey=" + newsApiKey;
            String json = restTemplate.getForObject(url, String.class);
            if (json == null) return false;
            JsonNode root = objectMapper.readTree(json);
            return root.path("totalResults").asInt(0) >= MIN_RESULTS;
        } catch (Exception e) {
            log.warn("NewsAPI cross-source failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean searchGuardian(String encodedQuery) {
        try {
            String url = "https://content.guardianapis.com/search?q=" + encodedQuery
                    + "&page-size=10&api-key=" + guardianKey;
            String json = restTemplate.getForObject(url, String.class);
            if (json == null) return false;
            JsonNode root = objectMapper.readTree(json);
            return root.path("response").path("total").asInt(0) >= MIN_RESULTS;
        } catch (Exception e) {
            log.warn("Guardian cross-source failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean searchGNews(String encodedQuery) {
        try {
            String url = "https://gnews.io/api/v4/search?q=" + encodedQuery
                    + "&lang=en&max=10&token=" + gnewsKey;
            String json = restTemplate.getForObject(url, String.class);
            if (json == null) return false;
            JsonNode root = objectMapper.readTree(json);
            return root.path("totalArticles").asInt(0) >= MIN_RESULTS;
        } catch (Exception e) {
            log.warn("GNews cross-source failed: {}", e.getMessage());
            return false;
        }
    }

    // ── витягування ключових слів ─────────────────────────────────

    String extractKeywords(String title) {
        if (title == null || title.isBlank()) return "";
        return Arrays.stream(
                        title.toLowerCase()
                                .replaceAll("[^a-z0-9\\s]", "")
                                .split("\\s+"))
                .filter(w -> w.length() >= 4)
                .filter(w -> !STOP_WORDS.contains(w))
                .limit(4)
                .collect(Collectors.joining(" "));
    }
}
