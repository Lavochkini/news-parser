package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LinguisticAnalyzer")
class LinguisticAnalyzerTest {

    private LinguisticAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new LinguisticAnalyzer();
    }

    // ── Helper ──────────────────────────────────────────────────────────
    private NewsItem item(String title, String description, String body) {
        return NewsItem.builder()
                .title(title)
                .description(description)
                .fullContent(body)
                .url("https://example.com/test")
                .build();
    }

    private NewsItem titleOnly(String title) {
        return item(title, null, null);
    }

    // ── Базові значення ─────────────────────────────────────────────────

    @Test
    @DisplayName("Чиста стаття → максимальний бал 35 (без штрафів)")
    void cleanArticle_maxScore() {
        // Заголовок поділяє ключові слова з тілом (overlap ≥ 40%) + 3+ цитати + без негативних патернів
        NewsItem news = item(
                "Economy growth confirmed by experts",
                "Experts confirmed economy growth rate. According to Dr. Reynolds study published in Nature.",
                "The economy growth confirmed at the press conference by leading experts. Study results valid."
        );
        LinguisticResult result = analyzer.analyze(news);

        // Citation density ≥3 → 0; headline overlap ≥ 40% → 0; no bad patterns → penalty = 0
        assertThat(result.getScore()).isEqualTo(35);
        assertThat(result.getHedgeWordsFound()).isEmpty();
        assertThat(result.getClickbaitIndicators()).isEmpty();
        assertThat(result.getEmotionalWordsFound()).isEmpty();
        assertThat(result.getManipulationIndicators()).isEmpty();
        assertThat(result.getConspiracyIndicators()).isEmpty();
    }

    // ── Хедж-слова ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Хедж-слова")
    class HedgeWords {

        @Test
        @DisplayName("EN: 'allegedly' виявляється у тексті")
        void english_hedgeWord_detected() {
            NewsItem news = item("News", null,
                    "The president allegedly signed the document without approval.");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.getHedgeWordsFound()).contains("allegedly");
            assertThat(r.getScore()).isLessThan(35);
        }

        @Test
        @DisplayName("UA: 'нібито' виявляється у тексті")
        void ukrainian_hedgeWord_detected() {
            NewsItem news = item("Новина", null,
                    "Президент нібито підписав документ без дозволу.");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.getHedgeWordsFound()).contains("нібито");
            assertThat(r.getScore()).isLessThan(35);
        }

        @Test
        @DisplayName("Декілька хедж-слів → штраф накопичується (макс -15)")
        void multiple_hedgeWords_cappedPenalty() {
            NewsItem news = item("News", null,
                    "Allegedly reportedly supposedly the minister perhaps maybe claimed unverified.");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.getHedgeWordsFound().size()).isGreaterThanOrEqualTo(5);
            // Max hedge penalty = 15; нижча межа з іншими можливими штрафами
            assertThat(r.getScore()).isLessThanOrEqualTo(35 - 15);
        }

        @Test
        @DisplayName("Хедж вимкнено у sub-options → список порожній")
        void disabled_hedgeWords_emptyList() {
            NewsItem news = item("News", null, "Allegedly the president signed.");
            var sub = new AnalysisOptions.LinguisticSubOptions(
                    false, true, true, true, true, true, true, true, true);
            LinguisticResult r = analyzer.analyze(news, sub);

            assertThat(r.getHedgeWordsFound()).isEmpty();
        }
    }

    // ── Клікбейт ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Клікбейт")
    class Clickbait {

        @Test
        @DisplayName("EN: 'shocking' у заголовку → виявляється")
        void english_clickbait_inTitle() {
            NewsItem news = titleOnly("Shocking revelation about the government");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.getClickbaitIndicators()).contains("shocking");
        }

        @Test
        @DisplayName("UA: 'сенсація' у заголовку → виявляється")
        void ukrainian_clickbait_inTitle() {
            NewsItem news = titleOnly("Сенсація: президент підписав таємний указ");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.getClickbaitIndicators()).contains("сенсація");
        }

        @Test
        @DisplayName("'!!' у заголовку → excessive punctuation")
        void excessivePunctuation_detected() {
            NewsItem news = titleOnly("Breaking news about the economy!!");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.getClickbaitIndicators()).contains("excessive punctuation");
        }

        @Test
        @DisplayName("ALL CAPS слово у заголовку → виявляється")
        void allCapsWord_detected() {
            NewsItem news = titleOnly("BREAKING: President signs new law");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.getClickbaitIndicators()).contains("ALL CAPS word");
        }

        @Test
        @DisplayName("Клікбейт лише у тілі (не заголовку) → НЕ виявляється")
        void clickbait_onlyInBody_notDetected() {
            NewsItem news = item("Normal title about economy",
                    "This is shocking and unbelievable content.", null);
            LinguisticResult r = analyzer.analyze(news);

            // Клікбейт перевіряється лише в заголовку
            assertThat(r.getClickbaitIndicators()).isEmpty();
        }
    }

    // ── Емоційна лексика ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Емоційна лексика")
    class EmotionalWords {

        @Test
        @DisplayName("EN: 'catastrophe' та 'chaos' виявляються")
        void english_emotionalWords_detected() {
            NewsItem news = item("News", "A catastrophe unfolded.",
                    "Complete chaos erupted in the streets amid outrage.");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.getEmotionalWordsFound()).containsAnyOf("catastrophe", "chaos", "outrage");
        }

        @Test
        @DisplayName("UA: 'зрада' та 'корупція' виявляються")
        void ukrainian_emotionalWords_detected() {
            NewsItem news = item("Новина",
                    "Корупція та зрада у владних структурах.",
                    "Скандал навколо фальсифікації документів.");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.getEmotionalWordsFound()).containsAnyOf("корупція", "зрада", "скандал");
        }
    }

    // ── Маніпуляції ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Маніпулятивні патерни")
    class Manipulation {

        @Test
        @DisplayName("EN: 'fake news' → маніпуляція виявлена")
        void english_fakeNews_detected() {
            NewsItem news = item("News", null,
                    "The article claims fake news media is hiding the truth.");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.getManipulationIndicators()).isNotEmpty();
        }

        @Test
        @DisplayName("EN: 'caught red-handed' → маніпуляція виявлена")
        void english_caughtRedHanded_detected() {
            NewsItem news = item("News", null,
                    "The minister was caught red-handed accepting bribes.");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.getManipulationIndicators()).isNotEmpty();
        }

        @Test
        @DisplayName("UA: 'реальні українці' → маніпуляція виявлена")
        void ukrainian_manipulation_detected() {
            NewsItem news = item("Новина", null,
                    "Всі справжні українці мають знати правду про владу.");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.getManipulationIndicators()).isNotEmpty();
        }
    }

    // ── Конспірологія ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Конспірологічні патерни")
    class Conspiracy {

        @Test
        @DisplayName("EN: 'deep state' у заголовку → конспірологія + cap 40")
        void english_deepState_detected_capsAt40() {
            // Беремо статтю з хорошим тілом, але з deep state у заголовку
            NewsItem news = item("Deep state plot exposed",
                    "Deep state agenda controls everything.",
                    "According to Dr. Smith confirmed study published in Nature.");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.getConspiracyIndicators()).isNotEmpty();
        }

        @Test
        @DisplayName("UA: 'глибока держава' → конспірологія виявлена")
        void ukrainian_deepState_detected() {
            NewsItem news = item("Глибока держава контролює Україну",
                    "Тіньовий уряд діє поза контролем.",
                    "Деталі змови приховані від громадськості.");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.getConspiracyIndicators()).isNotEmpty();
        }

        @Test
        @DisplayName("EN: 'false flag' → конспірологія виявлена")
        void english_falseFlag_detected() {
            NewsItem news = item("False flag operation",
                    "Crisis actor false flag suspected.", null);
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.getConspiracyIndicators()).isNotEmpty();
        }
    }

    // ── Анонімна атрибуція ───────────────────────────────────────────────

    @Nested
    @DisplayName("Анонімна атрибуція")
    class AnonymousSources {

        @Test
        @DisplayName("EN: 'sources say' → анонімне джерело виявлено")
        void english_sourcesSay_detected() {
            NewsItem news = item("News", null,
                    "Sources say the president will resign tomorrow.");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.getAnonymousSourcesFound()).isNotEmpty();
        }

        @Test
        @DisplayName("UA: 'джерела стверджують' → анонімне джерело виявлено")
        void ukrainian_sources_detected() {
            NewsItem news = item("Новина", null,
                    "Анонімні джерела стверджують що уряд готує нові закони.");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.getAnonymousSourcesFound()).isNotEmpty();
        }
    }

    // ── Щільність цитат ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Щільність цитат")
    class CitationDensity {

        @Test
        @DisplayName("0 цитат → штраф -6")
        void noCitations_penalty6() {
            NewsItem news = item("Sensational title", null,
                    "Someone said something happened somewhere sometime.");
            // Один хедж-штраф: "said" — але це не іменна атрибуція
            LinguisticResult r = analyzer.analyze(news,
                    new AnalysisOptions.LinguisticSubOptions(
                            false, false, false, false, false, false, true, false, false));
            // Тільки citationDensity увімкнено
            assertThat(r.getCitationCount()).isEqualTo(0);
            assertThat(r.getScore()).isEqualTo(35 - 6); // тільки цитаційний штраф
        }

        @Test
        @DisplayName("3+ цитат → штраф 0")
        void threePlusCitations_noPenalty() {
            NewsItem news = item(
                    "Scientific breakthrough confirmed",
                    "According to Dr. Smith the study published in Nature confirms results.",
                    "Professor Johnson confirmed at the press conference results are valid."
            );
            LinguisticResult r = analyzer.analyze(news,
                    new AnalysisOptions.LinguisticSubOptions(
                            false, false, false, false, false, false, true, false, false));

            assertThat(r.getCitationCount()).isGreaterThanOrEqualTo(3);
            assertThat(r.getScore()).isEqualTo(35); // без штрафу
        }
    }

    // ── Узгодженість заголовку ───────────────────────────────────────────

    @Nested
    @DisplayName("Узгодженість заголовку / тіла")
    class HeadlineConsistency {

        @Test
        @DisplayName("Повний збіг слів → overlap ≈ 1.0, штраф 0")
        void perfectOverlap_noPenalty() {
            NewsItem news = item(
                    "Economy recovery growth statistics",
                    "Economy recovery growth statistics are improving rapidly.",
                    null);
            LinguisticResult r = analyzer.analyze(news,
                    new AnalysisOptions.LinguisticSubOptions(
                            false, false, false, false, false, false, false, true, false));

            assertThat(r.getHeadlineBodyOverlap()).isGreaterThanOrEqualTo(0.80);
            assertThat(r.getScore()).isEqualTo(35);
        }

        @Test
        @DisplayName("Нульовий збіг → штраф -8")
        void zeroOverlap_maxPenalty() {
            NewsItem news = item(
                    "Elephant jungle safari adventure",
                    "Central bank interest rate monetary policy decision.",
                    "Federal reserve meets quarterly to adjust monetary policy decisions.");
            LinguisticResult r = analyzer.analyze(news,
                    new AnalysisOptions.LinguisticSubOptions(
                            false, false, false, false, false, false, false, true, false));

            assertThat(r.getHeadlineBodyOverlap()).isLessThan(0.20);
            assertThat(r.getScore()).isEqualTo(35 - 8);
        }

        @Test
        @DisplayName("Відсутнє тіло → overlap = 1.0, штраф 0")
        void noBody_nopenalty() {
            NewsItem news = item("Some headline about topic", null, null);
            LinguisticResult r = analyzer.analyze(news,
                    new AnalysisOptions.LinguisticSubOptions(
                            false, false, false, false, false, false, false, true, false));

            assertThat(r.getHeadlineBodyOverlap()).isEqualTo(1.0);
            assertThat(r.getScore()).isEqualTo(35);
        }
    }

    // ── Закон Беттерідж ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Закон Беттерідж")
    class Betteridge {

        @Test
        @DisplayName("EN: 'Is this real?' → виявляється")
        void english_questionTitle_detected() {
            NewsItem news = titleOnly("Is the government hiding the truth?");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.isBetteridgeDetected()).isTrue();
            assertThat(r.getScore()).isLessThan(35);
        }

        @Test
        @DisplayName("UA: 'Чи справді...' → виявляється")
        void ukrainian_questionTitle_detected() {
            NewsItem news = titleOnly("Чи справді Зеленський підписав таємний договір?");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.isBetteridgeDetected()).isTrue();
        }

        @Test
        @DisplayName("Стверджувальний заголовок → не виявляється")
        void declarativeTitle_notDetected() {
            NewsItem news = titleOnly("President signs new economic policy");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.isBetteridgeDetected()).isFalse();
        }

        @Test
        @DisplayName("Питання без знаку '?' → не виявляється")
        void questionWithoutMark_notDetected() {
            NewsItem news = titleOnly("Is the government hiding the truth");
            LinguisticResult r = analyzer.analyze(news);

            assertThat(r.isBetteridgeDetected()).isFalse();
        }
    }

    // ── Sub-options ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Sub-options")
    class SubOptions {

        @Test
        @DisplayName("LinguisticSubOptions.none() → базовий бал 35, всі списки порожні")
        void allDisabled_baseScore() {
            NewsItem news = item(
                    "Is everything allegedly a deep state conspiracy?!",
                    "Sources say fake news media is lying.",
                    "This is catastrophic outrage and manipulation.");
            LinguisticResult r = analyzer.analyze(news, AnalysisOptions.LinguisticSubOptions.none());

            assertThat(r.getScore()).isEqualTo(35);
            assertThat(r.getHedgeWordsFound()).isEmpty();
            assertThat(r.getClickbaitIndicators()).isEmpty();
            assertThat(r.getEmotionalWordsFound()).isEmpty();
            assertThat(r.getManipulationIndicators()).isEmpty();
            assertThat(r.getConspiracyIndicators()).isEmpty();
            assertThat(r.getAnonymousSourcesFound()).isEmpty();
            assertThat(r.isBetteridgeDetected()).isFalse();
        }

        @Test
        @DisplayName("LinguisticSubOptions.all() → всі підметоди активні")
        void allEnabled_processesEverything() {
            NewsItem news = item(
                    "Is allegedly the deep state real?",
                    "Sources say fake news media hides the truth.",
                    "Complete catastrophe and chaos outrage.");
            LinguisticResult r = analyzer.analyze(news, AnalysisOptions.LinguisticSubOptions.all());

            // Хоча б один підметод мав спрацювати
            assertThat(r.getScore()).isLessThan(35);
        }
    }
}
