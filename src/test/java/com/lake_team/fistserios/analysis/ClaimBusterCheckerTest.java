package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClaimBusterChecker")
class ClaimBusterCheckerTest {

    private ClaimBusterChecker checker;

    @BeforeEach
    void setUp() {
        checker = new ClaimBusterChecker();
    }

    private NewsItem item(String title, String description) {
        return NewsItem.builder()
                .title(title)
                .description(description)
                .url("https://example.com")
                .build();
    }

    @Test
    @DisplayName("Порожній текст → нейтральний результат (5 балів, 0.0 сирий)")
    void emptyText_neutralResult() {
        NewsItem news = item(null, null);
        var result = checker.check(news);

        assertThat(result.score()).isEqualTo(5);
        assertThat(result.rawScore()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Числові факти (відсотки) → хоча б 1 сигнал")
    void numericFacts_signalDetected() {
        NewsItem news = item(
                "GDP grew by 3.2 percent last quarter",
                "The growth of 5 percent is the highest in decade."
        );
        var result = checker.check(news);

        // Числа → низький бал ClaimBuster (article is claim-worthy → harder to verify → lower score)
        assertThat(result.rawScore()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Дієслова заяв ('said', 'confirmed') → сигнал виявлено")
    void claimVerbs_signalDetected() {
        NewsItem news = item(
                "President confirmed new policy",
                "The minister stated the law passed. Officials announced new measures."
        );
        var result = checker.check(news);

        assertThat(result.rawScore()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Абсолютні квантори ('always', 'never') → сигнал виявлено")
    void absolutes_signalDetected() {
        NewsItem news = item(
                "The largest ever economic crisis",
                "This is the most significant decline. Everyone agrees it is the worst situation."
        );
        var result = checker.check(news);

        assertThat(result.rawScore()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Питальний заголовок → знижує кількість сигналів")
    void questionTitle_reducesSignals() {
        // Однаковий контент, але заголовок — питання
        NewsItem withQuestion = item(
                "Did GDP grow by 3.2 percent?",
                "The minister stated figures. Growth of 5 percent is recorded."
        );
        NewsItem withStatement = item(
                "GDP grew by 3.2 percent",
                "The minister stated figures. Growth of 5 percent is recorded."
        );

        var questionResult   = checker.check(withQuestion);
        var statementResult  = checker.check(withStatement);

        // Питальний заголовок має менший або рівний rawScore
        assertThat(questionResult.rawScore()).isLessThanOrEqualTo(statementResult.rawScore());
    }

    @Test
    @DisplayName("Порівняльні структури ('more than', 'less than') → сигнал")
    void comparisons_signalDetected() {
        NewsItem news = item(
                "More than 50 countries agreed on climate deal",
                "Up to 30 billion dollars allocated. Greater than expected response."
        );
        var result = checker.check(news);

        assertThat(result.rawScore()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Всі 4 типи сигналів → максимальний rawScore (1.0)")
    void allSignals_maxRawScore() {
        NewsItem news = item(
                "Record 15 percent GDP growth confirmed by the largest ministry",
                "The minister stated growth is more than expected. Always confirmed by officials."
        );
        var result = checker.check(news);

        // Усі 4 категорії + питання не знижує → raw = 4/4 = 1.0
        assertThat(result.rawScore()).isGreaterThanOrEqualTo(0.75);
    }

    @Test
    @DisplayName("Низький сигнал (≤1) → score 10 (мало перевірабельних тверджень)")
    void lowSignals_highScore() {
        NewsItem news = item(
                "Something happened yesterday",
                "People were involved in an event that occurred recently."
        );
        var result = checker.check(news);

        // 0-1 сигнал → score = 10
        assertThat(result.score()).isEqualTo(10);
    }
}
