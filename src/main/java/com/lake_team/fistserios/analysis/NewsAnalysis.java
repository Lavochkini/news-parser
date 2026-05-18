package com.lake_team.fistserios.analysis;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "news_analysis")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NewsAnalysis {

    @Id
    private String id;

    @Indexed(unique = true)
    private String newsItemId;

    // ── Підсумковий score (0–100 коли всі шари активні) ──
    private int credibilityScore;

    // ── Шар 1: Лінгвістичний аналіз (0–35) ──
    private int linguisticScore;
    private List<String> hedgeWordsFound;
    private List<String> clickbaitIndicators;
    private List<String> emotionalWordsFound;
    private List<String> manipulationIndicators;
    private List<String> conspiracyIndicators;

    // ── Шар 2: Крос-перевірка (0–35) ──
    private int crossSourceScore;
    private int sourcesConfirmed;
    private List<String> confirmedSources;
    private String keywordsUsed;

    // ── Шар 3: Fact Check (0–30) ──
    private int factCheckScore;
    // підшар 3a: репутація домену (0–12)
    private int sourceReputationScore;
    private String sourceDomain;
    private String sourceReputationTier;
    // підшар 3b: ClaimBuster (0–10)
    private int claimBusterScore;
    private double claimBusterRaw;
    // підшар 3c: RSS Snopes/PolitiFact (0–8)
    private int rssCheckScore;
    private String rssMatchedTitle;
    private String rssMatchSource;
    // загальне
    private int factClaimsFound;
    private List<FactCheckClaim> factCheckClaims;
    private String factCheckVerdict;

    // ── Додаткові метрики (не впливають на credibilityScore) ──

    // Sentiment (AFINN лексикон)
    private double sentimentScore;   // -1.0 .. +1.0
    private String sentimentLabel;   // POSITIVE / NEUTRAL / NEGATIVE
    private int    sentimentRawSum;
    private int    sentimentWordsMatched;

    // Readability (Flesch Reading Ease)
    private double readabilityScore;         // 0–100
    private String readabilityLevel;         // VERY_EASY .. VERY_DIFFICULT
    private int    wordCount;
    private double avgWordsPerSentence;
    private double avgSyllablesPerWord;

    // Ручний аналіз — збережено ким і коли
    private String analyzedByUsername;   // null для авто-batch
    private boolean isManual;            // true якщо запущено вручну

    // Людський опис результату аналізу
    private String reasoning;

    @CreatedDate
    private LocalDateTime analyzedAt;
}
