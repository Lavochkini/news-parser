package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FactCheckRssChecker {

    private final RestTemplate restTemplate;

    private static final String SNOPES_RSS    = "https://www.snopes.com/feed/";
    private static final String POLITIFACT_RSS = "https://www.politifact.com/rss/all/";

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "in", "on", "at", "to", "for",
            "of", "with", "by", "is", "was", "were", "be", "has", "had",
            "this", "that", "they", "he", "she", "we", "you", "not", "no"
    );

    private static final Pattern TITLE_PATTERN =
            Pattern.compile("<title>(?:<!\\[CDATA\\[)?(.*?)(?:]]>)?</title>", Pattern.DOTALL);

    public record RssCheckResult(int score, String matchedTitle, String source) {}

    public RssCheckResult check(NewsItem item) {
        List<String> keywords = extractKeywords(item.getTitle());
        if (keywords.isEmpty()) return neutral();

        RssCheckResult snopes = searchFeed(SNOPES_RSS, "Snopes", keywords);
        if (snopes.matchedTitle() != null) return snopes;

        RssCheckResult politifact = searchFeed(POLITIFACT_RSS, "PolitiFact", keywords);
        if (politifact.matchedTitle() != null) return politifact;

        return neutral();
    }

    @Cacheable(value = "rssFeed", key = "#feedUrl")
    public List<String> fetchTitles(String feedUrl) {
        try {
            String xml = restTemplate.getForObject(feedUrl, String.class);
            if (xml == null) return List.of();

            List<String> titles = new ArrayList<>();
            Matcher m = TITLE_PATTERN.matcher(xml);
            boolean first = true;
            while (m.find()) {
                if (first) { first = false; continue; }
                titles.add(m.group(1).trim().toLowerCase());
            }
            return titles;
        } catch (Exception e) {
            log.debug("RSS fetch failed for {}: {}", feedUrl, e.getMessage());
            return List.of();
        }
    }

    private RssCheckResult searchFeed(String feedUrl, String sourceName, List<String> keywords) {
        List<String> titles = fetchTitles(feedUrl);

        for (String title : titles) {
            long matches = keywords.stream().filter(title::contains).count();
            if (matches >= 2) {
                int score = detectRating(title);
                log.debug("RSS match in {}: '{}' → score {}", sourceName, title, score);
                return new RssCheckResult(score, title, sourceName);
            }
        }
        return new RssCheckResult(6, null, null);
    }

    private int detectRating(String title) {
        if (title.contains("true") && !title.contains("false") && !title.contains("mostly false"))
            return 8;
        if (title.contains("mostly true"))  return 7;
        if (title.contains("half true"))    return 5;
        if (title.contains("mostly false")) return 2;
        if (title.contains("pants on fire") || title.contains("false") || title.contains("misleading"))
            return 0;
        return 4;
    }

    private List<String> extractKeywords(String title) {
        if (title == null || title.isBlank()) return List.of();
        return Arrays.stream(title.toLowerCase()
                        .replaceAll("[^a-z0-9\\s]", "")
                        .split("\\s+"))
                .filter(w -> w.length() >= 4)
                .filter(w -> !STOP_WORDS.contains(w))
                .limit(4)
                .collect(Collectors.toList());
    }

    private RssCheckResult neutral() {
        return new RssCheckResult(6, null, null);
    }
}
