package com.lake_team.fistserios.controller.rest;

import com.lake_team.fistserios.analysis.NewsAnalysis;
import com.lake_team.fistserios.analysis.NewsAnalysisRepository;
import com.lake_team.fistserios.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin
public class DashboardController {

    private final NewsRepository      newsRepository;
    private final NewsAnalysisRepository analysisRepository;

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        long totalArticles = newsRepository.count();
        List<NewsAnalysis> analyses = analysisRepository.findAll();
        long totalAnalyzed = analyses.size();

        Map<String, Object> stats = new LinkedHashMap<>();

        // ── Базові лічильники ──
        stats.put("totalArticles", totalArticles);
        stats.put("totalAnalyzed", totalAnalyzed);

        if (analyses.isEmpty()) return stats;

        // ── Середній credibility score ──
        double avgCredibility = analyses.stream()
                .mapToInt(NewsAnalysis::getCredibilityScore)
                .average().orElse(0);
        stats.put("avgCredibilityScore", Math.round(avgCredibility * 10.0) / 10.0);

        // ── Розподіл credibility по бакетах (0-20, 20-40...) ──
        Map<String, Long> credBuckets = new LinkedHashMap<>();
        credBuckets.put("0–20",   bucket(analyses, 0,  20));
        credBuckets.put("21–40",  bucket(analyses, 21, 40));
        credBuckets.put("41–60",  bucket(analyses, 41, 60));
        credBuckets.put("61–80",  bucket(analyses, 61, 80));
        credBuckets.put("81–100", bucket(analyses, 81, 100));
        stats.put("credibilityBuckets", credBuckets);

        // ── Sentiment breakdown ──
        Map<String, Long> sentiment = analyses.stream()
                .filter(a -> a.getSentimentLabel() != null)
                .collect(Collectors.groupingBy(NewsAnalysis::getSentimentLabel, Collectors.counting()));
        stats.put("sentimentBreakdown", sentiment);

        // ── Readability breakdown ──
        Map<String, Long> readability = analyses.stream()
                .filter(a -> a.getReadabilityLevel() != null && !a.getReadabilityLevel().equals("UNKNOWN"))
                .collect(Collectors.groupingBy(NewsAnalysis::getReadabilityLevel, Collectors.counting()));
        stats.put("readabilityBreakdown", readability);

        // ── Source reputation breakdown ──
        Map<String, Long> reputation = analyses.stream()
                .filter(a -> a.getSourceReputationTier() != null)
                .collect(Collectors.groupingBy(NewsAnalysis::getSourceReputationTier, Collectors.counting()));
        stats.put("reputationBreakdown", reputation);

        // ── Середній score по шарах ──
        double avgLinguistic  = analyses.stream().mapToInt(NewsAnalysis::getLinguisticScore).average().orElse(0);
        double avgCrossSource = analyses.stream().mapToInt(NewsAnalysis::getCrossSourceScore).average().orElse(0);
        double avgFactCheck   = analyses.stream().mapToInt(NewsAnalysis::getFactCheckScore).average().orElse(0);
        stats.put("layerAverages", Map.of(
                "linguistic",  Math.round(avgLinguistic  * 10.0) / 10.0,
                "crossSource", Math.round(avgCrossSource * 10.0) / 10.0,
                "factCheck",   Math.round(avgFactCheck   * 10.0) / 10.0
        ));

        // ── Топ-10 доменів за середнім score ──
        Map<String, Double> domainAvg = analyses.stream()
                .filter(a -> a.getSourceDomain() != null && !a.getSourceDomain().isBlank())
                .collect(Collectors.groupingBy(
                        NewsAnalysis::getSourceDomain,
                        Collectors.averagingInt(NewsAnalysis::getCredibilityScore)
                ));
        stats.put("avgScoreByDomain", domainAvg.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> Math.round(e.getValue() * 10.0) / 10.0,
                        (a, b) -> a, LinkedHashMap::new)));

        // ── Топ hedge words ──
        Map<String, Long> hedgeCounts = analyses.stream()
                .filter(a -> a.getHedgeWordsFound() != null)
                .flatMap(a -> a.getHedgeWordsFound().stream())
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));
        stats.put("topHedgeWords", hedgeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new)));

        return stats;
    }

    private long bucket(List<NewsAnalysis> list, int from, int to) {
        return list.stream()
                .filter(a -> a.getCredibilityScore() >= from && a.getCredibilityScore() <= to)
                .count();
    }
}
