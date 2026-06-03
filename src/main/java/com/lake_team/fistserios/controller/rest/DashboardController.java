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

        stats.put("totalArticles", totalArticles);
        stats.put("totalAnalyzed", totalAnalyzed);

        if (analyses.isEmpty()) return stats;

        double avgCredibility = analyses.stream()
                .mapToInt(NewsAnalysis::getCredibilityScore)
                .average().orElse(0);
        stats.put("avgCredibilityScore", Math.round(avgCredibility * 10.0) / 10.0);

        Map<String, Long> credBuckets = new LinkedHashMap<>();
        credBuckets.put("0–20",   bucket(analyses, 0,  20));
        credBuckets.put("21–40",  bucket(analyses, 21, 40));
        credBuckets.put("41–60",  bucket(analyses, 41, 60));
        credBuckets.put("61–80",  bucket(analyses, 61, 80));
        credBuckets.put("81–100", bucket(analyses, 81, 100));
        stats.put("credibilityBuckets", credBuckets);

        Map<String, Long> sentiment = analyses.stream()
                .filter(a -> a.getSentimentLabel() != null)
                .collect(Collectors.groupingBy(NewsAnalysis::getSentimentLabel, Collectors.counting()));
        stats.put("sentimentBreakdown", sentiment);

        Map<String, Long> readability = analyses.stream()
                .filter(a -> a.getReadabilityLevel() != null && !a.getReadabilityLevel().equals("UNKNOWN"))
                .collect(Collectors.groupingBy(NewsAnalysis::getReadabilityLevel, Collectors.counting()));
        stats.put("readabilityBreakdown", readability);

        Map<String, Long> reputation = analyses.stream()
                .filter(a -> a.getSourceReputationTier() != null)
                .collect(Collectors.groupingBy(NewsAnalysis::getSourceReputationTier, Collectors.counting()));
        stats.put("reputationBreakdown", reputation);

        double avgLinguistic  = analyses.stream().mapToInt(NewsAnalysis::getLinguisticScore).average().orElse(0);
        double avgCrossSource = analyses.stream().mapToInt(NewsAnalysis::getCrossSourceScore).average().orElse(0);
        double avgFactCheck   = analyses.stream().mapToInt(NewsAnalysis::getFactCheckScore).average().orElse(0);
        stats.put("layerAverages", Map.of(
                "linguistic",  Math.round(avgLinguistic  * 10.0) / 10.0,
                "crossSource", Math.round(avgCrossSource * 10.0) / 10.0,
                "factCheck",   Math.round(avgFactCheck   * 10.0) / 10.0
        ));

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

    @GetMapping("/search")
    public Map<String, Object> searchArticles(
            @RequestParam(defaultValue = "0")   int scoreMin,
            @RequestParam(defaultValue = "100")  int scoreMax,
            @RequestParam(required = false)      String sourceType,
            @RequestParam(required = false)      String sentimentLabel,
            @RequestParam(required = false)      String readabilityLevel,
            @RequestParam(required = false)      String reputationTier,
            @RequestParam(required = false)      String search,
            @RequestParam(defaultValue = "credibility") String sortBy,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size) {

        List<NewsAnalysis> filtered = analysisRepository.findAll().stream()
                .filter(a -> a.getCredibilityScore() >= scoreMin && a.getCredibilityScore() <= scoreMax)
                .filter(a -> blank(sentimentLabel)   || sentimentLabel.equals(a.getSentimentLabel()))
                .filter(a -> blank(readabilityLevel) || readabilityLevel.equals(a.getReadabilityLevel()))
                .filter(a -> blank(reputationTier)   || reputationTier.equals(a.getSourceReputationTier()))
                .toList();

        String searchLc = blank(search) ? null : search.toLowerCase();
        List<Map<String, Object>> results = filtered.stream()
                .map(a -> {
                    Map<String, Object> dto = new LinkedHashMap<>();
                    dto.put("newsItemId",       a.getNewsItemId());
                    dto.put("credibilityScore", a.getCredibilityScore());
                    dto.put("linguisticScore",  a.getLinguisticScore());
                    dto.put("crossSourceScore", a.getCrossSourceScore());
                    dto.put("factCheckScore",   a.getFactCheckScore());
                    dto.put("sentimentLabel",   a.getSentimentLabel());
                    dto.put("readabilityLevel", a.getReadabilityLevel());
                    dto.put("reputationTier",   a.getSourceReputationTier());
                    dto.put("sourceDomain",     a.getSourceDomain());
                    dto.put("analyzedAt",       a.getAnalyzedAt());
                    newsRepository.findById(a.getNewsItemId()).ifPresent(item -> {
                        dto.put("title",       item.getTitle());
                        dto.put("source",      item.getSource());
                        dto.put("sourceType",  item.getSourceType() != null ? item.getSourceType().name() : null);
                        dto.put("url",         item.getUrl());
                        dto.put("publishedAt", item.getPublishedAt());
                        dto.put("category",    item.getCategory());
                        dto.put("datasetLabel",item.getDatasetLabel());
                        dto.put("imageUrl",    item.getImageUrl());
                        dto.put("description", item.getDescription());
                        dto.put("fullContent", item.getFullContent());
                    });
                    return dto;
                })
                .filter(dto -> {
                    if (!blank(sourceType) && !sourceType.equals(dto.get("sourceType"))) return false;
                    if (searchLc != null) {
                        Object t = dto.get("title");
                        if (t == null || !t.toString().toLowerCase().contains(searchLc)) return false;
                    }
                    return true;
                })
                .sorted(comparator(sortBy))
                .toList();

        int total     = results.size();
        int fromIdx   = page * size;
        int toIdx     = Math.min(fromIdx + size, total);
        List<Map<String, Object>> pageItems = fromIdx < total ? results.subList(fromIdx, toIdx) : List.of();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("total",      total);
        resp.put("page",       page);
        resp.put("size",       size);
        resp.put("totalPages", (int) Math.ceil((double) total / size));
        resp.put("results",    pageItems);
        return resp;
    }

    private Comparator<Map<String, Object>> comparator(String sortBy) {
        return switch (sortBy) {
            case "linguistic"  -> Comparator.comparingInt(m -> -(int) m.getOrDefault("linguisticScore",  0));
            case "crossSource" -> Comparator.comparingInt(m -> -(int) m.getOrDefault("crossSourceScore", 0));
            case "factCheck"   -> Comparator.comparingInt(m -> -(int) m.getOrDefault("factCheckScore",   0));
            case "credibility_asc" -> Comparator.comparingInt(m -> (int) m.getOrDefault("credibilityScore", 0));
            default            -> Comparator.comparingInt(m -> -(int) m.getOrDefault("credibilityScore", 0));
        };
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }

    private long bucket(List<NewsAnalysis> list, int from, int to) {
        return list.stream()
                .filter(a -> a.getCredibilityScore() >= from && a.getCredibilityScore() <= to)
                .count();
    }
}