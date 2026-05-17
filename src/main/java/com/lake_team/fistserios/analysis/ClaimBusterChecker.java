package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Оцінює "check-worthiness" статті без зовнішнього API.
 *
 * Шукає лінгвістичні ознаки перевірюваних тверджень у заголовку та описі:
 *   + числа / статистика / відсотки
 *   + абсолютні твердження (never, always, first, largest...)
 *   + конкретні твердження з суб'єктом ("X said", "X confirmed")
 *   - питальні речення (не твердження)
 *   - суто описові заголовки
 *
 * Score (0–10):
 *   0–1 ознак → 10  (нейтральний текст)
 *   2–3 ознаки → 6  (є перевірювані заяви)
 *   4+  ознаки → 2  (багато тверджень — варто перевірити)
 */
@Component
public class ClaimBusterChecker {

    // Числа, відсотки, суми
    private static final Pattern NUMBERS =
            Pattern.compile("\\b\\d+([.,]\\d+)?\\s*(%|percent|billion|million|thousand|dollars|euros)\\b",
                    Pattern.CASE_INSENSITIVE);

    // Абсолютні твердження
    private static final Pattern ABSOLUTES =
            Pattern.compile("\\b(never|always|every|all|none|no one|everyone|first ever|" +
                    "largest|smallest|highest|lowest|most|least|only|unique|record)\\b",
                    Pattern.CASE_INSENSITIVE);

    // Суб'єкт + дієслово твердження
    private static final Pattern CLAIM_VERBS =
            Pattern.compile("\\b(said|says|claimed|claims|confirmed|denied|denies|" +
                    "announced|stated|revealed|admitted|warned|argued|insisted)\\b",
                    Pattern.CASE_INSENSITIVE);

    // Порівняння
    private static final Pattern COMPARISONS =
            Pattern.compile("\\b(more than|less than|higher than|lower than|" +
                    "greater than|at least|up to|as much as|compared to)\\b",
                    Pattern.CASE_INSENSITIVE);

    // Питальні речення — знижують check-worthiness
    private static final Pattern QUESTION =
            Pattern.compile("\\?\\s*$");

    public record ClaimBusterResult(int score, double rawScore) {}

    public ClaimBusterResult check(NewsItem item) {
        String text = merge(item.getTitle(), item.getDescription());
        if (text.isBlank()) return neutral();

        int signals = 0;
        if (NUMBERS.matcher(text).find())    signals++;
        if (ABSOLUTES.matcher(text).find())  signals++;
        if (CLAIM_VERBS.matcher(text).find()) signals++;
        if (COMPARISONS.matcher(text).find()) signals++;

        // Питальний заголовок — зменшуємо сигнали
        if (item.getTitle() != null && QUESTION.matcher(item.getTitle().trim()).find()) {
            signals = Math.max(0, signals - 1);
        }

        double raw = signals / 4.0; // нормалізуємо до 0.0–1.0
        int score  = scoreFromSignals(signals);

        return new ClaimBusterResult(score, raw);
    }

    private int scoreFromSignals(int signals) {
        if (signals <= 1) return 10;
        if (signals <= 3) return 6;
        return 2;
    }

    private String merge(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) if (p != null) sb.append(p).append(' ');
        return sb.toString().toLowerCase();
    }

    private ClaimBusterResult neutral() {
        return new ClaimBusterResult(5, 0.0);
    }
}
