package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DatasetValidationService {

    private final NewsAnalysisRepository analysisRepository;
    private final NewsRepository         newsRepository;

    private static final int[] THRESHOLDS = {20, 25, 30, 35, 40, 45, 50, 55, 60};

    public Map<String, Object> validate() {
        // 1. Усі аналізи датасетних статей
        List<NewsAnalysis> all = analysisRepository.findAll();

        // 2. Збираємо пари (аналіз, мітка)
        record Pair(NewsAnalysis analysis, String label) {}

        List<Pair> pairs = all.stream()
                .map(a -> {
                    NewsItem item = newsRepository.findById(a.getNewsItemId()).orElse(null);
                    if (item == null || item.getDatasetLabel() == null) return null;
                    return new Pair(a, item.getDatasetLabel()); // "TRUE" або "FAKE"
                })
                .filter(Objects::nonNull)
                .toList();

        int total = pairs.size();
        if (total == 0) {
            return Map.of("error", "Немає проаналізованих датасетних статей. Спочатку запустіть аналіз.");
        }

        long totalTrue = pairs.stream().filter(p -> "TRUE".equals(p.label())).count();
        long totalFake = pairs.stream().filter(p -> "FAKE".equals(p.label())).count();

        // Розподіл балів
        Map<String, List<Integer>> scoresByLabel = new LinkedHashMap<>();
        scoresByLabel.put("TRUE", pairs.stream().filter(p -> "TRUE".equals(p.label()))
                .map(p -> p.analysis().getCredibilityScore()).collect(Collectors.toList()));
        scoresByLabel.put("FAKE", pairs.stream().filter(p -> "FAKE".equals(p.label()))
                .map(p -> p.analysis().getCredibilityScore()).collect(Collectors.toList()));

        double avgTrue = scoresByLabel.get("TRUE").stream().mapToInt(i -> i).average().orElse(0);
        double avgFake = scoresByLabel.get("FAKE").stream().mapToInt(i -> i).average().orElse(0);

        // Гістограма по бакетах для кожного класу
        Map<String, Object> distribution = buildDistribution(scoresByLabel);

        // 3. Метрики по кожному порогу
        List<Map<String, Object>> thresholdResults = new ArrayList<>();
        Map<String, Object> bestResult = null;
        double bestF1 = -1;

        for (int t : THRESHOLDS) {
            int tp = 0, tn = 0, fp = 0, fn = 0;
            for (Pair p : pairs) {
                boolean predTrue = p.analysis().getCredibilityScore() > t;
                boolean actTrue  = "TRUE".equals(p.label());
                if (predTrue && actTrue)  tp++;
                else if (!predTrue && !actTrue) tn++;
                else if (predTrue && !actTrue)  fp++;
                else                             fn++;
            }

            double accuracy  = (double)(tp + tn) / total;
            double precision = (tp + fp) == 0 ? 0 : (double) tp / (tp + fp);
            double recall    = (tp + fn) == 0 ? 0 : (double) tp / (tp + fn);
            double f1        = (precision + recall) == 0 ? 0
                    : 2 * precision * recall / (precision + recall);
            double specificity = (tn + fp) == 0 ? 0 : (double) tn / (tn + fp);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("threshold",   t);
            row.put("tp", tp); row.put("tn", tn); row.put("fp", fp); row.put("fn", fn);
            row.put("accuracy",    round2(accuracy));
            row.put("precision",   round2(precision));
            row.put("recall",      round2(recall));
            row.put("f1",          round2(f1));
            row.put("specificity", round2(specificity));
            thresholdResults.add(row);

            if (f1 > bestF1) { bestF1 = f1; bestResult = row; }
        }

        // 4. Складаємо відповідь
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalAnalyzed",  total);
        result.put("totalTrue",      totalTrue);
        result.put("totalFake",      totalFake);
        result.put("avgScoreTrue",   round2(avgTrue));
        result.put("avgScoreFake",   round2(avgFake));
        result.put("distribution",   distribution);
        result.put("thresholdResults", thresholdResults);
        result.put("bestThreshold",  bestResult);
        return result;
    }

    private Map<String, Object> buildDistribution(Map<String, List<Integer>> byLabel) {
        String[] buckets = {"0–20","21–40","41–60","61–80","81–100"};
        int[][] ranges   = {{0,20},{21,40},{41,60},{61,80},{81,100}};

        Map<String, Object> dist = new LinkedHashMap<>();
        dist.put("labels", Arrays.asList(buckets));

        for (Map.Entry<String, List<Integer>> entry : byLabel.entrySet()) {
            List<Long> counts = new ArrayList<>();
            for (int[] r : ranges) {
                int lo = r[0], hi = r[1];
                long c = entry.getValue().stream().filter(s -> s >= lo && s <= hi).count();
                counts.add(c);
            }
            dist.put(entry.getKey(), counts);
        }
        return dist;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
