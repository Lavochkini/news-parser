package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    // ── Підозрілі TLD ──────────────────────────────────────────────────
    private static final Set<String> SUSPICIOUS_TLDS = Set.of(
            ".xyz", ".click", ".info", ".top", ".tk", ".ml", ".ga", ".cf",
            ".gq", ".pw", ".buzz", ".win", ".loan", ".download", ".stream"
    );

    // ── Відомі бренди для перевірки мімікрії ──────────────────────────
    private static final Set<String> KNOWN_BRANDS = Set.of(
            "cnn", "bbc", "reuters", "apnews", "nytimes", "foxnews",
            "nbcnews", "abcnews", "cbsnews", "msnbc", "usatoday", "guardian"
    );

    public record ReputationResult(int score, String tier, String domain) {}

    public record UrlAnalysisResult(String tier, List<String> warnings, int penalty) {}

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

    /**
     * Розширений URL-аналіз: підозрілі TLD, мімікрія бренду, надмірні субдомени, IP-хости.
     * Повертає tier (CLEAN/SUSPICIOUS/FAKE), список попереджень і штраф (0, -2, -4, -5).
     */
    public UrlAnalysisResult checkUrl(String url) {
        if (url == null || url.isBlank()) {
            return new UrlAnalysisResult("UNKNOWN", List.of("No URL provided"), 0);
        }

        List<String> warnings = new ArrayList<>();
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) {
                return new UrlAnalysisResult("UNKNOWN", List.of("Malformed URL"), 0);
            }

            String domain = host.startsWith("www.") ? host.substring(4) : host;
            String[] parts = domain.split("\\.");

            // 1. IP-адреса як хост
            if (host.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                warnings.add("IP address used as host");
            }

            // 2. Підозрілий TLD
            for (String tld : SUSPICIOUS_TLDS) {
                if (domain.endsWith(tld)) {
                    warnings.add("Suspicious TLD: " + tld);
                    break;
                }
            }

            // 3. Мімікрія відомого бренду (abcnews.com.co, cnn-news.net тощо)
            boolean isKnownGood = HIGH.contains(domain) || GOOD.contains(domain);
            if (!isKnownGood) {
                for (String brand : KNOWN_BRANDS) {
                    if (domain.contains(brand)) {
                        warnings.add("Mimics well-known brand: " + brand);
                        break;
                    }
                }
            }

            // 4. Надмірна кількість субдоменів (більше 2 рівнів)
            int subdomainDepth = parts.length - 2; // наприклад news.today.site.com → 2
            if (subdomainDepth > 2) {
                warnings.add("Excessive subdomains (" + subdomainDepth + " levels)");
            }

            // 5. Дуже довге ім'я домену (обфускація)
            if (parts.length >= 2 && parts[parts.length - 2].length() > 30) {
                warnings.add("Unusually long domain name");
            }

        } catch (Exception e) {
            return new UrlAnalysisResult("UNKNOWN", List.of("Could not parse URL: " + e.getMessage()), 0);
        }

        if (warnings.isEmpty())      return new UrlAnalysisResult("CLEAN",      List.of(), 0);
        if (warnings.size() == 1)    return new UrlAnalysisResult("SUSPICIOUS", warnings,  -2);
        if (warnings.size() == 2)    return new UrlAnalysisResult("SUSPICIOUS", warnings,  -4);
        return                              new UrlAnalysisResult("FAKE",        warnings,  -5);
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
