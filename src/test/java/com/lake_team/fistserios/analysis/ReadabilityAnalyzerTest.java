package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ReadabilityAnalyzerTest {

    private ReadabilityAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new ReadabilityAnalyzer();
    }

    private NewsItem item(String title, String description) {
        NewsItem item = new NewsItem();
        item.setTitle(title);
        item.setDescription(description);
        return item;
    }

    @Test
    void analyze_emptyItem_shouldReturnUnknownLevel() {
        NewsItem item = new NewsItem();
        ReadabilityAnalyzer.ReadabilityResult result = analyzer.analyze(item);
        assertThat(result.level()).isEqualTo("UNKNOWN");
        assertThat(result.wordCount()).isEqualTo(0);
    }

    @Test
    void analyze_shortSimpleText_shouldHavePositiveFleschScore() {
        NewsItem item = item("The cat sat on the mat.", null);
        ReadabilityAnalyzer.ReadabilityResult result = analyzer.analyze(item);
        assertThat(result.fleschScore()).isGreaterThan(0);
        assertThat(result.wordCount()).isGreaterThan(0);
    }

    @Test
    void analyze_fleschScore_shouldBeBetween0And100() {
        NewsItem item = item("Breaking news about economy today.", "Experts say the situation is complicated.");
        ReadabilityAnalyzer.ReadabilityResult result = analyzer.analyze(item);
        assertThat(result.fleschScore()).isBetween(0.0, 100.0);
    }

    @ParameterizedTest
    @CsvSource({
            "1, a",
            "1, cat",
            "1, make",
            "3, beautiful"
    })
    void countSyllables_shouldReturnExpectedCount(int expected, String word) {
        assertThat(analyzer.countSyllables(word)).isEqualTo(expected);
    }

    @Test
    void analyze_wordAndSentenceCount_shouldMatchText() {
        NewsItem item = item("One two three. Four five six.", null);
        ReadabilityAnalyzer.ReadabilityResult result = analyzer.analyze(item);
        assertThat(result.wordCount()).isEqualTo(6);
        assertThat(result.sentenceCount()).isEqualTo(2);
    }

    @Test
    void analyze_levelField_shouldBeOneOfKnownValues() {
        NewsItem item = item("Cat sat on the mat.", null);
        String level = analyzer.analyze(item).level();
        assertThat(level).isIn("VERY_EASY", "EASY", "STANDARD", "DIFFICULT", "VERY_DIFFICULT", "UNKNOWN");
    }
}
