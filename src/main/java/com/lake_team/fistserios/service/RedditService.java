package com.lake_team.fistserios.service;

import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.model.NewsSourceType;
import com.lake_team.fistserios.model.reddit.RedditChild;
import com.lake_team.fistserios.model.reddit.RedditListing;
import com.lake_team.fistserios.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Парсить пости з Reddit (без API ключа).
 *
 * Суbreddit-и:
 *   r/news        — новини США, зазвичай якісні джерела
 *   r/worldnews   — міжнародні новини
 *   r/conspiracy  — конспірологія, часто ненадійні домени
 *
 * Reddit вимагає User-Agent у заголовку, інакше повертає 429.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedditService {

    private final NewsRepository newsRepository;
    private final RestTemplate   restTemplate;

    private static final String USER_AGENT = "NewsFeedApp/1.0 (thesis project)";
    private static final List<String> SUBREDDITS = List.of(
            "news", "worldnews", "conspiracy"
    );

    public List<NewsItem> fetchAndSaveAll() {
        return SUBREDDITS.stream()
                .flatMap(sub -> fetchSubreddit(sub).stream())
                .collect(Collectors.toList());
    }

    public List<NewsItem> fetchSubreddit(String subreddit) {
        try {
            String url = "https://www.reddit.com/r/" + subreddit + "/new.json?limit=50";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<RedditListing> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, RedditListing.class);

            if (response.getBody() == null || response.getBody().getData() == null) {
                log.warn("Empty response from r/{}", subreddit);
                return Collections.emptyList();
            }

            List<RedditChild> children = response.getBody().getData().getChildren();
            if (children == null) return Collections.emptyList();

            List<NewsItem> saved = children.stream()
                    .map(RedditChild::getData)
                    .filter(p -> p != null && !p.isSelf()) // тільки link-пости
                    .filter(p -> p.getUrl() != null && p.getUrl().startsWith("http"))
                    .filter(p -> p.getTitle() != null && !p.getTitle().isBlank())
                    .map(p -> mapToNewsItem(p, subreddit))
                    .map(this::saveIfNotExists)
                    .collect(Collectors.toList());

            log.info("Reddit r/{}: fetched {} articles", subreddit, saved.size());
            return saved;

        } catch (Exception e) {
            log.error("Error fetching r/{}: {}", subreddit, e.getMessage());
            return Collections.emptyList();
        }
    }

    private NewsItem mapToNewsItem(com.lake_team.fistserios.model.reddit.RedditPost post,
                                   String subreddit) {
        return NewsItem.builder()
                .title(post.getTitle())
                .description(post.getSelftext() != null && !post.getSelftext().isBlank()
                        ? truncate(post.getSelftext(), 500) : null)
                .url(post.getUrl())
                .source(post.getDomain() != null ? post.getDomain() : "reddit.com")
                .category(subreddit)
                .sourceType(NewsSourceType.REDDIT)
                .publishedAt(fromUnix(post.getCreatedUtc()))
                .build();
    }

    private NewsItem saveIfNotExists(NewsItem item) {
        return newsRepository.findByUrl(item.getUrl())
                .orElseGet(() -> newsRepository.save(item));
    }

    private LocalDateTime fromUnix(double utc) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond((long) utc), ZoneOffset.UTC);
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    @Scheduled(cron = "0 30 * * * *") // кожну годину в :30
    public void scheduledFetch() {
        log.info("Scheduled Reddit fetch...");
        fetchAndSaveAll();
    }
}
