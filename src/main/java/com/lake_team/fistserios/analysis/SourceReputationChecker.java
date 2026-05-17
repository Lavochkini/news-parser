package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * Оцінює довіру до джерела на основі домену статті.
 * Методологія: Media Bias / AllSides domain reputation tiers.
 *
 * Score (0–12):
 *   HIGH    → 12   відомі авторитетні видання / офіційні організації
 *   GOOD    → 9    якісні ЗМІ з редакційними стандартами
 *   UNKNOWN → 5    невідомий домен
 *   LOW     → 0    відомі ненадійні / фейкові джерела
 */
@Component
public class SourceReputationChecker {

    private static final Set<String> HIGH = Set.of(
            "reuters.com", "apnews.com", "bbc.com", "bbc.co.uk",
            "npr.org", "pbs.org", "who.int", "un.org", "europa.eu",
            "wsj.com", "ft.com", "economist.com", "nature.com",
            "science.org", "newscientist.com", "scientificamerican.com",
            "ap.org", "afp.com"
    );

    private static final Set<String> GOOD = Set.of(
            "theguardian.com", "nytimes.com", "washingtonpost.com",
            "bloomberg.com", "politico.com", "theatlantic.com",
            "time.com", "newsweek.com", "usatoday.com", "latimes.com",
            "chicagotribune.com", "nbcnews.com", "cbsnews.com",
            "abcnews.go.com", "forbes.com", "bbc.co.uk",
            "independent.co.uk", "telegraph.co.uk", "thetimes.co.uk",
            "dw.com", "france24.com", "euronews.com"
    );

    private static final Set<String> LOW = Set.of(
            "infowars.com", "naturalnews.com", "beforeitsnews.com",
            "worldnewsdailyreport.com", "empirenews.net", "nationalreport.net",
            "theonion.com", "babylonbee.com", "stuppid.com", "huzlers.com",
            "abcnews.com.co", "cbsnews.com.co", "usatoday.com.co"
    );

    public record ReputationResult(int score, String tier, String domain) {}

    public ReputationResult check(NewsItem item) {
        String domain = extractDomain(item.getUrl());

        // .gov та .edu завжди HIGH
        if (domain.endsWith(".gov") || domain.endsWith(".edu")) {
            return new ReputationResult(12, "HIGH", domain);
        }

        if (HIGH.contains(domain))    return new ReputationResult(12, "HIGH",    domain);
        if (GOOD.contains(domain))    return new ReputationResult(9,  "GOOD",    domain);
        if (LOW.contains(domain))     return new ReputationResult(0,  "LOW",     domain);
        return                               new ReputationResult(5,  "UNKNOWN", domain);
    }

    private String extractDomain(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) return "";
            // прибрати "www."
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return "";
        }
    }
}
