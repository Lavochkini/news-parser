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

    private int credibilityScore;

    private int linguisticScore;

    private List<String> hedgeWordsFound;
    private List<String> clickbaitIndicators;
    private List<String> emotionalWordsFound;
    private List<String> manipulationIndicators;
    private List<String> conspiracyIndicators;

    private List<String> anonymousSourcesFound;
    private int          citationCount;
    private double       headlineBodyOverlap;
    private boolean      betteridgeDetected;

    private int crossSourceScore;
    private int sourcesConfirmed;
    private List<String> confirmedSources;
    private String keywordsUsed;

    private int factCheckScore;

    private int    sourceReputationScore;
    private String sourceDomain;
    private String sourceReputationTier;

    private String       urlAnalysisTier;
    private List<String> urlWarnings;
    private int          urlPenalty;

    private int    claimBusterScore;
    private double claimBusterRaw;

    private int    rssCheckScore;
    private String rssMatchedTitle;
    private String rssMatchSource;

    private boolean recycledNewsDetected;
    private String  recycledNewsReason;
    private int     recycledNewsPenalty;

    private int                 factClaimsFound;
    private List<FactCheckClaim> factCheckClaims;
    private String              factCheckVerdict;

    private double sentimentScore;
    private String sentimentLabel;
    private int    sentimentRawSum;
    private int    sentimentWordsMatched;

    private double readabilityScore;
    private String readabilityLevel;
    private int    wordCount;
    private double avgWordsPerSentence;
    private double avgSyllablesPerWord;

    private List<String> wikiEntitiesChecked;
    private List<String> wikiEntitiesFound;
    private int          wikiEntitiesTotal;

    private String detectedLanguage;
    private String languageNote;

    private String  analyzedByUsername;
    private boolean isManual;
    private String  reasoning;

    @CreatedDate
    private LocalDateTime analyzedAt;
}
