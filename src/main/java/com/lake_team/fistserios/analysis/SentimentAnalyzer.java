package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

/**
 * Аналіз тональності тексту на основі лексикону AFINN-111.
 *
 * Кожне слово лексикону має оцінку від -5 (дуже негативне)
 * до +5 (дуже позитивне). Сума оцінок нормалізується до [-1.0, +1.0].
 *
 * Результат:
 *   sentimentScore  — нормалізований: -1.0 (негатив) .. +1.0 (позитив)
 *   sentimentLabel  — POSITIVE / NEUTRAL / NEGATIVE
 */
@Component
public class SentimentAnalyzer {

    // Підмножина AFINN-111, релевантна для новинних текстів
    private static final Map<String, Integer> AFINN = Map.ofEntries(
            // Сильно негативні (-4 .. -5)
            Map.entry("terrorism",   -5), Map.entry("terrorist",  -5),
            Map.entry("massacre",    -5), Map.entry("genocide",   -5),
            Map.entry("atrocity",    -5), Map.entry("torture",    -5),
            Map.entry("murder",      -4), Map.entry("assassinate",-4),
            Map.entry("catastrophe", -4), Map.entry("devastate",  -4),
            Map.entry("corruption",  -4), Map.entry("fraud",      -4),
            Map.entry("criminal",    -4), Map.entry("brutal",     -4),

            // Негативні (-2 .. -3)
            Map.entry("war",         -3), Map.entry("conflict",   -2),
            Map.entry("crisis",      -3), Map.entry("disaster",   -3),
            Map.entry("attack",      -2), Map.entry("threat",     -2),
            Map.entry("fear",        -2), Map.entry("panic",      -2),
            Map.entry("violence",    -3), Map.entry("terror",     -3),
            Map.entry("fail",        -2), Map.entry("failure",    -2),
            Map.entry("wrong",       -2), Map.entry("lie",        -3),
            Map.entry("fake",        -3), Map.entry("false",      -2),
            Map.entry("scandal",     -3), Map.entry("problem",    -1),
            Map.entry("risk",        -2), Map.entry("danger",     -2),
            Map.entry("loss",        -2), Map.entry("defeat",     -2),
            Map.entry("collapse",    -3), Map.entry("crash",      -2),
            Map.entry("bomb",        -3), Map.entry("illegal",    -2),
            Map.entry("ban",         -1), Map.entry("decline",    -1),
            Map.entry("death",       -2), Map.entry("kill",       -3),
            Map.entry("hurt",        -2), Map.entry("damage",     -2),
            Map.entry("abuse",       -3), Map.entry("exploit",    -2),
            Map.entry("depressing",  -2), Map.entry("alarming",   -2),
            Map.entry("outrage",     -3), Map.entry("shocking",   -2),
            Map.entry("controversial",-1),Map.entry("dispute",    -1),
            Map.entry("tension",     -2), Map.entry("protest",    -1),

            // Нейтрально-позитивні (+1)
            Map.entry("change",       1), Map.entry("reform",      1),
            Map.entry("increase",     1), Map.entry("rise",        1),
            Map.entry("gain",         1), Map.entry("deal",        1),
            Map.entry("stable",       1), Map.entry("agree",       1),
            Map.entry("support",      1), Map.entry("fund",        1),

            // Позитивні (+2 .. +3)
            Map.entry("peace",        2), Map.entry("success",     2),
            Map.entry("victory",      2), Map.entry("win",         2),
            Map.entry("progress",     2), Map.entry("growth",      2),
            Map.entry("improve",      2), Map.entry("agreement",   2),
            Map.entry("help",         2), Map.entry("save",        2),
            Map.entry("recover",      2), Map.entry("recovery",    2),
            Map.entry("protect",      2), Map.entry("good",        2),
            Map.entry("benefit",      2), Map.entry("hope",        2),
            Map.entry("safe",         2), Map.entry("positive",    2),
            Map.entry("opportunity",  2), Map.entry("cooperation", 2),

            // Сильно позитивні (+4 .. +5)
            Map.entry("breakthrough", 4), Map.entry("outstanding", 3),
            Map.entry("excellent",    3), Map.entry("great",       3),
            Map.entry("historic",     2), Map.entry("celebrate",   3),
            Map.entry("liberate",     3), Map.entry("triumph",     4),
            Map.entry("revolutionary",3), Map.entry("miracle",     4)
    );

    public record SentimentResult(double score, String label, int rawSum, int wordsMatched) {}

    public SentimentResult analyze(NewsItem item) {
        String text = merge(item.getTitle(), item.getDescription(), item.getFullContent());
        if (text.isBlank()) return new SentimentResult(0.0, "NEUTRAL", 0, 0);

        String[] words = text.toLowerCase().replaceAll("[^a-z\\s]", "").split("\\s+");

        int sum     = 0;
        int matched = 0;
        for (String word : words) {
            Integer val = AFINN.get(word);
            if (val != null) {
                sum += val;
                matched++;
            }
        }

        if (matched == 0) return new SentimentResult(0.0, "NEUTRAL", 0, 0);

        // Нормалізуємо: ділимо на (matched * 5) — макс можливий sum
        double normalized = Math.max(-1.0, Math.min(1.0, sum / (matched * 5.0)));
        String label = normalized > 0.1 ? "POSITIVE" : normalized < -0.1 ? "NEGATIVE" : "NEUTRAL";

        return new SentimentResult(normalized, label, sum, matched);
    }

    private String merge(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) if (p != null) sb.append(p).append(' ');
        return sb.toString();
    }
}
