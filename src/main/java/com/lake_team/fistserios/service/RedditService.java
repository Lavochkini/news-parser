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

@Service
@RequiredArgsConstructor
@Slf4j
public class RedditService {

    private final NewsRepository newsRepository;
    private final RestTemplate   restTemplate;

    private static final String USER_AGENT = "web:com.lake_team.news-parser:1.0 (by /u/news_parser_bot)";
    private static final List<String> SUBREDDITS = List.of(
            "news", "worldnews", "conspiracy"
    );

    private static final int TARGET_TOTAL   = 200;
    private static final int PER_PAGE_LIMIT = 100; // Reddit API maximum per request

    public List<NewsItem> fetchAndSaveAll() {
        List<NewsItem> all = SUBREDDITS.stream()
                .flatMap(sub -> fetchSubreddit(sub).stream())
                .collect(Collectors.toList());
        log.info("Reddit total saved: {}", all.size());
        return all;
    }

    public List<NewsItem> fetchSubreddit(String subreddit) {
        int perSubTarget = (int) Math.ceil((double) TARGET_TOTAL / SUBREDDITS.size());
        return fetchSubredditPaged(subreddit, perSubTarget);
    }

    private List<NewsItem> fetchSubredditPaged(String subreddit, int target) {
        List<NewsItem> result = new java.util.ArrayList<>();
        String after = null;

        while (result.size() < target) {
            String url = "https://www.reddit.com/r/" + subreddit + "/new.json?limit=" + PER_PAGE_LIMIT
                    + (after != null ? "&after=" + after : "");
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", USER_AGENT);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<RedditListing> response = restTemplate.exchange(
                        url, HttpMethod.GET, entity, RedditListing.class);

                if (response.getBody() == null || response.getBody().getData() == null) {
                    log.warn("Empty response from r/{}", subreddit);
                    break;
                }

                var data     = response.getBody().getData();
                var children = data.getChildren();
                if (children == null || children.isEmpty()) break;

                List<NewsItem> page = children.stream()
                        .map(RedditChild::getData)
                        .filter(p -> p != null && !p.isSelf())
                        .filter(p -> p.getUrl() != null && p.getUrl().startsWith("http"))
                        .filter(p -> p.getTitle() != null && !p.getTitle().isBlank())
                        .map(p -> mapToNewsItem(p, subreddit))
                        .map(this::saveIfNotExists)
                        .collect(Collectors.toList());

                result.addAll(page);
                after = data.getAfter();
                log.info("Reddit r/{}: page fetched {}, total so far {}", subreddit, page.size(), result.size());

                if (after == null) break; // no more pages

                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }

            } catch (Exception e) {
                log.error("Error fetching r/{}: {}", subreddit, e.getMessage());
                break;
            }
        }

        log.info("Reddit r/{}: finished with {} articles", subreddit, result.size());
        return result;
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
