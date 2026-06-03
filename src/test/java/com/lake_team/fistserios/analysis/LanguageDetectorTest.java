package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.lake_team.fistserios.analysis.LanguageDetector.Language.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LanguageDetector")
class LanguageDetectorTest {

    private LanguageDetector detector;

    @BeforeEach
    void setUp() {
        detector = new LanguageDetector();
    }

    // ── detect(String) ──────────────────────────────────────────────────

    @Test
    @DisplayName("Англійський текст → ENGLISH")
    void english_text_detected() {
        String text = "The president of the United States announced a new economic policy yesterday morning.";
        assertThat(detector.detect(text)).isEqualTo(ENGLISH);
    }

    @Test
    @DisplayName("Ukrainian text with і/ї → UKRAINIAN")
    void ukrainian_text_detected() {
        // Містить і (U+0456) та ї (U+0457) — унікальні для UA
        String text = "Президент України їде до Польщі. Він підписав важливі документи у Києві.";
        assertThat(detector.detect(text)).isEqualTo(UKRAINIAN);
    }

    @Test
    @DisplayName("Ukrainian text with є → UKRAINIAN")
    void ukrainian_text_withYe_detected() {
        String text = "Це є важливе рішення для всієї країни та її громадян.";
        assertThat(detector.detect(text)).isEqualTo(UKRAINIAN);
    }

    @Test
    @DisplayName("Російський текст з ъ → RUSSIAN")
    void russian_text_detected() {
        // Містить ъ (U+044A) — унікальний для RU
        String text = "Президент России объявил о новой политике. Объём торговли ежедневно растёт.";
        assertThat(detector.detect(text)).isEqualTo(RUSSIAN);
    }

    @Test
    @DisplayName("Змішаний текст (EN+UA) → MIXED")
    void mixed_text_detected() {
        // Достатньо кирилиці для MIXED (15-50%), але < 50% для повної кирилиці
        String text = "NATO conference безпека обговорення питань discusses security policy matters.";
        LanguageDetector.Language lang = detector.detect(text);
        assertThat(lang).isEqualTo(MIXED);
    }

    @Test
    @DisplayName("Дуже короткий текст (< 15 літер) → UNKNOWN")
    void short_text_unknown() {
        assertThat(detector.detect("Hi")).isEqualTo(UNKNOWN);
        assertThat(detector.detect("")).isEqualTo(UNKNOWN);
        assertThat(detector.detect((String) null)).isEqualTo(UNKNOWN);
    }

    @Test
    @DisplayName("Порожній рядок → UNKNOWN")
    void blank_text_unknown() {
        assertThat(detector.detect("   ")).isEqualTo(UNKNOWN);
    }

    // ── detect(NewsItem) ────────────────────────────────────────────────

    @Test
    @DisplayName("NewsItem з українським заголовком → UKRAINIAN")
    void newsItem_ukrainian_detected() {
        NewsItem item = NewsItem.builder()
                .title("Президент України їде до Брюсселя")
                .description("Важливий візит для підписання угоди")
                .url("https://example.com")
                .build();
        assertThat(detector.detect(item)).isEqualTo(UKRAINIAN);
    }

    @Test
    @DisplayName("NewsItem з англійським контентом → ENGLISH")
    void newsItem_english_detected() {
        NewsItem item = NewsItem.builder()
                .title("President signs new economic bill")
                .description("The legislation passed with bipartisan support")
                .fullContent("The bill addresses infrastructure spending across multiple sectors.")
                .url("https://example.com")
                .build();
        assertThat(detector.detect(item)).isEqualTo(ENGLISH);
    }

    // ── isEnglishCompatible ─────────────────────────────────────────────

    @Test
    @DisplayName("ENGLISH і UNKNOWN → сумісні")
    void english_and_unknown_compatible() {
        assertThat(detector.isEnglishCompatible(ENGLISH)).isTrue();
        assertThat(detector.isEnglishCompatible(UNKNOWN)).isTrue();
    }

    @Test
    @DisplayName("UKRAINIAN, RUSSIAN, MIXED → не сумісні")
    void non_english_not_compatible() {
        assertThat(detector.isEnglishCompatible(UKRAINIAN)).isFalse();
        assertThat(detector.isEnglishCompatible(RUSSIAN)).isFalse();
        assertThat(detector.isEnglishCompatible(MIXED)).isFalse();
    }

    // ── buildNote ───────────────────────────────────────────────────────

    @Test
    @DisplayName("ENGLISH → null (дисклеймер не потрібен)")
    void buildNote_english_null() {
        assertThat(detector.buildNote(ENGLISH)).isNull();
    }

    @Test
    @DisplayName("UNKNOWN → null")
    void buildNote_unknown_null() {
        assertThat(detector.buildNote(UNKNOWN)).isNull();
    }

    @Test
    @DisplayName("UKRAINIAN → непорожній рядок з поясненням")
    void buildNote_ukrainian_nonEmpty() {
        String note = detector.buildNote(UKRAINIAN);
        assertThat(note).isNotBlank();
        assertThat(note).contains("українська");
    }

    @Test
    @DisplayName("RUSSIAN → непорожній рядок")
    void buildNote_russian_nonEmpty() {
        assertThat(detector.buildNote(RUSSIAN)).isNotBlank();
    }

    @Test
    @DisplayName("MIXED → непорожній рядок")
    void buildNote_mixed_nonEmpty() {
        assertThat(detector.buildNote(MIXED)).isNotBlank();
    }
}
