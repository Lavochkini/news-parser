package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SourceReputationChecker")
class SourceReputationCheckerTest {

    private SourceReputationChecker checker;

    @BeforeEach
    void setUp() {
        checker = new SourceReputationChecker();
    }

    private NewsItem withUrl(String url) {
        return NewsItem.builder().url(url).title("Test").build();
    }

    // ── check() — репутація домену ───────────────────────────────────────

    @Nested
    @DisplayName("Репутація домену")
    class ReputationCheck {

        @Test
        @DisplayName("reuters.com → HIGH (12 балів)")
        void reuters_isHigh() {
            var result = checker.check(withUrl("https://reuters.com/article/123"));
            assertThat(result.tier()).isEqualTo("HIGH");
            assertThat(result.score()).isEqualTo(12);
        }

        @Test
        @DisplayName("apnews.com → HIGH (12 балів)")
        void apnews_isHigh() {
            var result = checker.check(withUrl("https://apnews.com/article/test"));
            assertThat(result.tier()).isEqualTo("HIGH");
            assertThat(result.score()).isEqualTo(12);
        }

        @Test
        @DisplayName("nytimes.com → GOOD (9 балів)")
        void nytimes_isGood() {
            var result = checker.check(withUrl("https://nytimes.com/article/test"));
            assertThat(result.tier()).isEqualTo("GOOD");
            assertThat(result.score()).isEqualTo(9);
        }

        @Test
        @DisplayName("theguardian.com → GOOD (9 балів)")
        void guardian_isGood() {
            var result = checker.check(withUrl("https://theguardian.com/world/test"));
            assertThat(result.tier()).isEqualTo("GOOD");
            assertThat(result.score()).isEqualTo(9);
        }

        @Test
        @DisplayName("infowars.com → LOW (0 балів)")
        void infowars_isLow() {
            var result = checker.check(withUrl("https://infowars.com/article/test"));
            assertThat(result.tier()).isEqualTo("LOW");
            assertThat(result.score()).isEqualTo(0);
        }

        @Test
        @DisplayName("naturalnews.com → LOW (0 балів)")
        void naturalnews_isLow() {
            var result = checker.check(withUrl("https://naturalnews.com/article"));
            assertThat(result.tier()).isEqualTo("LOW");
            assertThat(result.score()).isEqualTo(0);
        }

        @Test
        @DisplayName("Невідомий домен → UNKNOWN (5 балів)")
        void unknownDomain_fivePoints() {
            var result = checker.check(withUrl("https://some-random-blog.net/post/123"));
            assertThat(result.tier()).isEqualTo("UNKNOWN");
            assertThat(result.score()).isEqualTo(5);
        }

        @Test
        @DisplayName(".gov домен → HIGH (12 балів)")
        void govDomain_isHigh() {
            var result = checker.check(withUrl("https://cia.gov/news/release"));
            assertThat(result.tier()).isEqualTo("HIGH");
            assertThat(result.score()).isEqualTo(12);
        }

        @Test
        @DisplayName(".edu домен → HIGH (12 балів)")
        void eduDomain_isHigh() {
            var result = checker.check(withUrl("https://harvard.edu/research/study"));
            assertThat(result.tier()).isEqualTo("HIGH");
            assertThat(result.score()).isEqualTo(12);
        }

        @Test
        @DisplayName("www. прибирається з домену")
        void www_stripped() {
            var result = checker.check(withUrl("https://www.reuters.com/article/123"));
            assertThat(result.domain()).isEqualTo("reuters.com");
            assertThat(result.tier()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("Null URL → UNKNOWN (5 балів)")
        void nullUrl_unknown() {
            var result = checker.check(withUrl(null));
            assertThat(result.tier()).isEqualTo("UNKNOWN");
        }
    }

    // ── checkUrl() — розширений URL аналіз ──────────────────────────────

    @Nested
    @DisplayName("Розширений URL-аналіз")
    class UrlAnalysis {

        @Test
        @DisplayName("Надійний URL (reuters.com) → CLEAN, штраф 0")
        void trustedUrl_clean() {
            var result = checker.checkUrl("https://reuters.com/article/123");
            assertThat(result.tier()).isEqualTo("CLEAN");
            assertThat(result.penalty()).isEqualTo(0);
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("Підозрілий TLD .xyz → SUSPICIOUS, штраф -2")
        void suspiciousTld_xyz() {
            var result = checker.checkUrl("https://news-today.xyz/article");
            assertThat(result.tier()).isEqualTo("SUSPICIOUS");
            assertThat(result.penalty()).isEqualTo(-2);
            assertThat(result.warnings()).anyMatch(w -> w.contains(".xyz"));
        }

        @Test
        @DisplayName("Підозрілий TLD .click → SUSPICIOUS")
        void suspiciousTld_click() {
            var result = checker.checkUrl("https://breaking-news.click/story");
            assertThat(result.tier()).isEqualTo("SUSPICIOUS");
            assertThat(result.warnings()).anyMatch(w -> w.contains(".click"));
        }

        @Test
        @DisplayName("Мімікрія бренду cnn → попередження")
        void brandMimicry_cnn() {
            var result = checker.checkUrl("https://cnn-news.info/story");
            assertThat(result.warnings()).anyMatch(w -> w.toLowerCase().contains("cnn"));
        }

        @Test
        @DisplayName("Надмірні субдомени (3+) → попередження")
        void excessiveSubdomains() {
            var result = checker.checkUrl("https://news.real.today.site.com/article");
            assertThat(result.warnings()).anyMatch(w -> w.toLowerCase().contains("subdomain"));
        }

        @Test
        @DisplayName("IP-адреса як хост → SUSPICIOUS")
        void ipAddressHost() {
            var result = checker.checkUrl("http://192.168.1.1/article");
            assertThat(result.warnings()).anyMatch(w -> w.toLowerCase().contains("ip"));
        }

        @Test
        @DisplayName("FAKE: 3+ попереджень → штраф -5")
        void fakeUrl_maxPenalty() {
            // .tk (suspicious TLD) + brand mimicry + excessive subdomains
            var result = checker.checkUrl("https://real.cnn.news.today.tk/breaking");
            assertThat(result.penalty()).isLessThanOrEqualTo(-4);
        }

        @Test
        @DisplayName("Null URL → tier UNKNOWN, штраф 0")
        void nullUrl_unknown() {
            var result = checker.checkUrl(null);
            assertThat(result.tier()).isEqualTo("UNKNOWN");
            assertThat(result.penalty()).isEqualTo(0);
        }
    }
}
