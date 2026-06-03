package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SentimentAnalyzerTest {

    private SentimentAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new SentimentAnalyzer();
    }

    private NewsItem item(String title, String description) {
        NewsItem item = new NewsItem();
        item.setTitle(title);
        item.setDescription(description);
        return item;
    }

    @Test
    void analyze_blankText_shouldReturnNeutral() {
        NewsItem item = new NewsItem();
        SentimentAnalyzer.SentimentResult result = analyzer.analyze(item);
        assertThat(result.label()).isEqualTo("NEUTRAL");
        assertThat(result.score()).isEqualTo(0.0);
    }

    @Test
    void analyze_positiveText_shouldReturnPositiveLabel() {
        NewsItem item = item("Peace agreement brings hope and recovery", "Great progress and outstanding success");
        SentimentAnalyzer.SentimentResult result = analyzer.analyze(item);
        assertThat(result.label()).isEqualTo("POSITIVE");
        assertThat(result.score()).isGreaterThan(0.1);
    }

    @Test
    void analyze_negativeText_shouldReturnNegativeLabel() {
        NewsItem item = item("Terrorism and genocide: massacre of civilians", "Brutal murder, catastrophe and fraud");
        SentimentAnalyzer.SentimentResult result = analyzer.analyze(item);
        assertThat(result.label()).isEqualTo("NEGATIVE");
        assertThat(result.score()).isLessThan(-0.1);
    }

    @Test
    void analyze_scoreShouldBeNormalizedBetweenMinusOneAndOne() {
        NewsItem item = item("War crisis disaster terror violence", "attack threat fear panic collapse crash");
        SentimentAnalyzer.SentimentResult result = analyzer.analyze(item);
        assertThat(result.score()).isBetween(-1.0, 1.0);
    }

    @Test
    void analyze_matchedWordCount_shouldBePositive() {
        NewsItem item = item("peace victory success", null);
        SentimentAnalyzer.SentimentResult result = analyzer.analyze(item);
        assertThat(result.wordsMatched()).isGreaterThan(0);
    }
}
