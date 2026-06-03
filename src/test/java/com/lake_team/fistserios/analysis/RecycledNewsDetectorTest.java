package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecycledNewsDetector")
class RecycledNewsDetectorTest {

    private RecycledNewsDetector detector;

    @BeforeEach
    void setUp() {
        detector = new RecycledNewsDetector();
    }

    private NewsItem.NewsItemBuilder base() {
        return NewsItem.builder()
                .title("Test article")
                .url("https://example.com/article");
    }

    // ── Ознака 1: рік у URL ─────────────────────────────────────────────

    @Test
    @DisplayName("URL містить /2019/, опублікована 2024 → перевидана (різниця > 1р)")
    void urlYear_olderThanPublished_detected() {
        NewsItem item = base()
                .url("https://example.com/news/2019/03/article-about-economy")
                .publishedAt(LocalDateTime.of(2024, 5, 1, 10, 0))
                .build();
        var result = detector.detect(item);

        assertThat(result.detected()).isTrue();
        assertThat(result.penalty()).isEqualTo(-5);
        assertThat(result.reason()).contains("2019");
    }

    @Test
    @DisplayName("URL-рік збігається з роком публікації → не перевидана")
    void urlYear_matchesPublished_notDetected() {
        NewsItem item = base()
                .url("https://example.com/news/2024/03/article")
                .publishedAt(LocalDateTime.of(2024, 5, 1, 10, 0))
                .build();
        var result = detector.detect(item);

        assertThat(result.detected()).isFalse();
        assertThat(result.penalty()).isEqualTo(0);
    }

    @Test
    @DisplayName("URL-рік на 1 рік старший → допустимо, не перевидана")
    void urlYear_oneYearOlder_notDetected() {
        NewsItem item = base()
                .url("https://example.com/news/2023/12/article")
                .publishedAt(LocalDateTime.of(2024, 5, 1, 10, 0))
                .build();
        var result = detector.detect(item);

        // різниця = 1, порог > 1 → не виявляється
        assertThat(result.detected()).isFalse();
    }

    // ── Ознака 2: явні дати у тексті ────────────────────────────────────

    @Test
    @DisplayName("2+ старих дати у тексті (January 2018, March 2017) → перевидана")
    void oldDatesInText_detected() {
        NewsItem item = base()
                .publishedAt(LocalDateTime.of(2024, 6, 1, 10, 0))
                .fullContent("In January 2018 the crisis began. By March 2017 things were already bad. " +
                             "The economic data from 2016 shows a clear pattern.")
                .build();
        var result = detector.detect(item);

        assertThat(result.detected()).isTrue();
    }

    @Test
    @DisplayName("Актуальні дати у тексті → не перевидана")
    void recentDatesInText_notDetected() {
        NewsItem item = base()
                .publishedAt(LocalDateTime.of(2024, 6, 1, 10, 0))
                .fullContent("The meeting in January 2024 confirmed the policy. " +
                             "On 2024-03-15 the agreement was signed.")
                .build();
        var result = detector.detect(item);

        assertThat(result.detected()).isFalse();
    }

    // ── Ознака 3: рік у заголовку ────────────────────────────────────────

    @Test
    @DisplayName("Рік у заголовку набагато старший → перевидана")
    void oldYearInTitle_detected() {
        NewsItem item = base()
                .title("2018 Economic Crisis: What Happened and Why")
                .publishedAt(LocalDateTime.of(2024, 6, 1, 10, 0))
                .build();
        var result = detector.detect(item);

        assertThat(result.detected()).isTrue();
        assertThat(result.reason()).contains("2018");
    }

    // ── Граничні випадки ─────────────────────────────────────────────────

    @Test
    @DisplayName("publishedAt = null → не перевидана (штраф 0)")
    void nullPublishedAt_notDetected() {
        NewsItem item = base()
                .url("https://example.com/news/2015/article")
                .publishedAt(null)
                .build();
        var result = detector.detect(item);

        assertThat(result.detected()).isFalse();
        assertThat(result.penalty()).isEqualTo(0);
    }

    @Test
    @DisplayName("URL без року → не перевидана")
    void urlWithoutYear_notDetected() {
        NewsItem item = base()
                .url("https://example.com/article-about-economy")
                .publishedAt(LocalDateTime.of(2024, 5, 1, 10, 0))
                .build();
        var result = detector.detect(item);

        assertThat(result.detected()).isFalse();
    }

    @Test
    @DisplayName("Виявлена перевидана новина → reason не null")
    void detected_reasonNotNull() {
        NewsItem item = base()
                .url("https://example.com/news/2018/01/article")
                .publishedAt(LocalDateTime.of(2024, 5, 1, 10, 0))
                .build();
        var result = detector.detect(item);

        assertThat(result.detected()).isTrue();
        assertThat(result.reason()).isNotNull();
    }
}
