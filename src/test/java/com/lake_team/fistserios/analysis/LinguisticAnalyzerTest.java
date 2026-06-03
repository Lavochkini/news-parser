package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LinguisticAnalyzerTest {

    private LinguisticAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new LinguisticAnalyzer();
    }

    private NewsItem item(String title, String description) {
        NewsItem item = new NewsItem();
        item.setTitle(title);
        item.setDescription(description);
        return item;
    }

    @Test
    void analyze_cleanText_shouldReturnMaxScore() {
        NewsItem item = item("Parliament approves national budget", "Lawmakers voted on new financial plan.");
        LinguisticResult result = analyzer.analyze(item);
        assertThat(result.getScore()).isEqualTo(35);
        assertThat(result.getHedgeWordsFound()).isEmpty();
        assertThat(result.getClickbaitIndicators()).isEmpty();
    }

    @Test
    void analyze_hedgeWords_shouldReduceScore() {
        NewsItem item = item("Allegedly corrupt official reportedly fled", "Possibly unverified claims, maybe unclear evidence.");
        LinguisticResult result = analyzer.analyze(item);
        assertThat(result.getScore()).isLessThan(35);
        assertThat(result.getHedgeWordsFound()).isNotEmpty();
    }

    @Test
    void analyze_clickbaitTitle_shouldDetectIndicators() {
        NewsItem item = item("SHOCKING secret revealed! You won't believe this", null);
        LinguisticResult result = analyzer.analyze(item);
        assertThat(result.getClickbaitIndicators()).isNotEmpty();
    }

    @Test
    void analyze_allCapsWord_shouldBeDetectedAsClickbait() {
        NewsItem item = item("BREAKING news today", null);
        LinguisticResult result = analyzer.analyze(item);
        assertThat(result.getClickbaitIndicators()).contains("ALL CAPS word");
    }

    @Test
    void analyze_excessivePunctuation_shouldBeDetectedAsClickbait() {
        NewsItem item = item("Amazing!! Must read this now??", null);
        LinguisticResult result = analyzer.analyze(item);
        assertThat(result.getClickbaitIndicators()).contains("excessive punctuation");
    }

    @Test
    void analyze_emotionalWords_shouldReduceScore() {
        NewsItem item = item("Catastrophic disaster causes outrage", "Horrifying atrocity and chaos ensued.");
        LinguisticResult result = analyzer.analyze(item);
        assertThat(result.getEmotionalWordsFound()).isNotEmpty();
        assertThat(result.getScore()).isLessThan(35);
    }

    @Test
    void analyze_conspiracyPatterns_shouldApplyLargePenalty() {
        NewsItem item = item("Elvis is still alive and was secretly replaced", null);
        LinguisticResult result = analyzer.analyze(item);
        assertThat(result.getConspiracyIndicators()).isNotEmpty();
        // conspiracy penalty = 20, so max score = 35 - 20 = 15
        assertThat(result.getScore()).isLessThanOrEqualTo(15);
    }

    @Test
    void analyze_score_shouldNeverBeNegative() {
        NewsItem item = item(
                "SHOCKING!! Crisis actor false flag illuminati conspiracy",
                "Everyone knows the deep state is hiding the hidden truth. " +
                "Allegedly reportedly supposedly unverified unconfirmed claimed."
        );
        LinguisticResult result = analyzer.analyze(item);
        assertThat(result.getScore()).isGreaterThanOrEqualTo(0);
    }
}
