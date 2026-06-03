package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Визначає, чи є стаття «переробленою» (старою) новиною, виданою знову як нова.
 *
 * <p>Три евристики:
 * <ol>
 *   <li><b>URL-рік</b> — рік у шляху URL різниться більш ніж на 1 рік від дати публікації.</li>
 *   <li><b>Явні дати в тексті</b> — у тексті знайдено дати, що на 1+ рік старші за publishedAt.</li>
 *   <li><b>Стара дата у заголовку</b> — у заголовку є рік, значно старший за рік публікації.</li>
 * </ol>
 */
@Component
public class RecycledNewsDetector {

    // Рік у шляху URL: /2019/03/article, /news/2018-07/...
    private static final Pattern URL_YEAR_PATTERN =
            Pattern.compile("/(20[0-2]\\d)/");

    // Явні дати в тексті вигляду: "January 2019", "March 15, 2018", "2017-08-23"
    private static final Pattern TEXT_DATE_YEAR =
            Pattern.compile(
                    "(?:January|February|March|April|May|June|July|August|September|October|November|December)" +
                    "\\s+(?:\\d{1,2},?\\s+)?(20[0-2]\\d)" +
                    "|\\b(20[0-2]\\d)[-/]\\d{2}[-/]\\d{2}\\b" +
                    "|\\b\\d{1,2}[-/]\\d{1,2}[-/](20[0-2]\\d)\\b",
                    Pattern.CASE_INSENSITIVE);

    // Рік у заголовку (4-значне число в діапазоні 2000–2029)
    private static final Pattern TITLE_YEAR = Pattern.compile("\\b(20[0-2]\\d)\\b");

    /** Результат перевірки */
    public record RecycledResult(boolean detected, String reason, int penalty) {}

    public RecycledResult detect(NewsItem item) {
        LocalDateTime publishedAt = item.getPublishedAt();
        int pubYear = (publishedAt != null) ? publishedAt.getYear() : -1;

        List<String> reasons = new ArrayList<>();

        // ── 1. Рік у URL ──
        if (item.getUrl() != null) {
            Matcher m = URL_YEAR_PATTERN.matcher(item.getUrl());
            if (m.find()) {
                try {
                    int urlYear = Integer.parseInt(m.group(1));
                    if (pubYear > 0 && (pubYear - urlYear) > 1) {
                        reasons.add("URL contains year " + urlYear + " but article published in " + pubYear);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // ── 2. Явні дати в тексті ──
        String fullText = merge(item.getTitle(), item.getDescription(), item.getFullContent());
        if (pubYear > 0 && !fullText.isBlank()) {
            Matcher m = TEXT_DATE_YEAR.matcher(fullText);
            int oldDatesCount = 0;
            while (m.find()) {
                String yearStr = firstNonNull(m.group(1), m.group(2), m.group(3));
                if (yearStr != null) {
                    try {
                        int mentionedYear = Integer.parseInt(yearStr);
                        if ((pubYear - mentionedYear) > 1) {
                            oldDatesCount++;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (oldDatesCount >= 2) {
                reasons.add("Text contains " + oldDatesCount + " dates significantly older than publication date");
            }
        }

        // ── 3. Рік у заголовку ──
        if (item.getTitle() != null && pubYear > 0) {
            Matcher m = TITLE_YEAR.matcher(item.getTitle());
            while (m.find()) {
                try {
                    int titleYear = Integer.parseInt(m.group(1));
                    if ((pubYear - titleYear) > 1) {
                        reasons.add("Title contains old year (" + titleYear + ") while article published in " + pubYear);
                        break;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (reasons.isEmpty()) {
            return new RecycledResult(false, null, 0);
        }
        String reason = String.join("; ", reasons);
        return new RecycledResult(true, reason, -5);
    }

    private String merge(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) if (p != null) sb.append(p).append(' ');
        return sb.toString();
    }

    private String firstNonNull(String... values) {
        for (String v : values) if (v != null) return v;
        return null;
    }
}
