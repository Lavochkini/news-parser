package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsAnalysisService {

    private final NewsAnalysisRepository analysisRepository;
    private final NewsRepository newsRepository;
    private final LinguisticAnalyzer linguisticAnalyzer;
    private final CrossSourceAnalyzer crossSourceAnalyzer;
    private final FactCheckAnalyzer factCheckAnalyzer;
    private final SentimentAnalyzer sentimentAnalyzer;
    private final ReadabilityAnalyzer readabilityAnalyzer;

    /**
     * Аналізує одну статтю. Якщо аналіз вже існує — повертає кешований результат.
     */
    public NewsAnalysis analyzeArticle(String newsItemId) {
        return analysisRepository.findByNewsItemId(newsItemId)
                .orElseGet(() -> runAnalysis(newsItemId));
    }

    /**
     * Повторний аналіз — ігнорує існуючий результат.
     */
    public NewsAnalysis reanalyzeArticle(String newsItemId) {
        analysisRepository.findByNewsItemId(newsItemId)
                .ifPresent(a -> analysisRepository.deleteById(a.getId()));
        return runAnalysis(newsItemId);
    }

    /**
     * Асинхронний batch-аналіз всіх статей без результату.
     */
    @Async
    public CompletableFuture<Void> analyzeAllAsync() {
        log.info("Starting batch analysis...");
        int page = 0;
        int processed = 0;

        Page<NewsItem> batch;
        do {
            batch = newsRepository.findAllByOrderByPublishedAtDesc(
                    PageRequest.of(page++, 50, Sort.by("publishedAt").descending()));

            for (NewsItem item : batch.getContent()) {
                if (!analysisRepository.existsByNewsItemId(item.getId())) {
                    try {
                        runAnalysis(item.getId());
                        processed++;
                    } catch (Exception e) {
                        log.warn("Failed to analyze article {}: {}", item.getId(), e.getMessage());
                    }
                }
            }
        } while (batch.hasNext());

        log.info("Batch analysis complete. Processed: {}", processed);
        return CompletableFuture.completedFuture(null);
    }

    public Optional<NewsAnalysis> getByNewsItemId(String newsItemId) {
        return analysisRepository.findByNewsItemId(newsItemId);
    }

    @Async
    public CompletableFuture<Void> forceReanalyzeAllAsync() {
        log.info("Force batch: clearing all analyses...");
        analysisRepository.deleteAll();
        return analyzeAllAsync();
    }

    // ─────────────────────────────────────────────
    private NewsAnalysis runAnalysis(String newsItemId) {
        NewsItem item = newsRepository.findById(newsItemId)
                .orElseThrow(() -> new IllegalArgumentException("NewsItem not found: " + newsItemId));

        LinguisticResult              linguistic   = linguisticAnalyzer.analyze(item);
        CrossSourceResult             crossSource  = crossSourceAnalyzer.analyze(item);
        FactCheckLayerResult          factCheck    = factCheckAnalyzer.analyze(item);
        SentimentAnalyzer.SentimentResult sentiment  = sentimentAnalyzer.analyze(item);
        ReadabilityAnalyzer.ReadabilityResult readability = readabilityAnalyzer.analyze(item);

        boolean conspiracyDetected = !linguistic.getConspiracyIndicators().isEmpty();

        int effectiveCrossSourceScore = conspiracyDetected ? 0 : crossSource.getScore();
        int rawScore = linguistic.getScore() + effectiveCrossSourceScore + factCheck.getScore();
        int credibilityScore = conspiracyDetected ? Math.min(rawScore, 40) : rawScore;

        NewsAnalysis analysis = NewsAnalysis.builder()
                .newsItemId(newsItemId)
                .credibilityScore(credibilityScore)
                .linguisticScore(linguistic.getScore())
                .hedgeWordsFound(linguistic.getHedgeWordsFound())
                .clickbaitIndicators(linguistic.getClickbaitIndicators())
                .emotionalWordsFound(linguistic.getEmotionalWordsFound())
                .conspiracyIndicators(linguistic.getConspiracyIndicators())
                .crossSourceScore(effectiveCrossSourceScore)
                .sourcesConfirmed(crossSource.getSourcesConfirmed())
                .confirmedSources(crossSource.getConfirmedSources())
                .keywordsUsed(crossSource.getKeywordsUsed())
                .factCheckScore(factCheck.getScore())
                .sourceReputationScore(factCheck.getSourceReputationScore())
                .sourceDomain(factCheck.getSourceDomain())
                .sourceReputationTier(factCheck.getSourceReputationTier())
                .claimBusterScore(factCheck.getClaimBusterScore())
                .claimBusterRaw(factCheck.getClaimBusterRaw())
                .rssCheckScore(factCheck.getRssCheckScore())
                .rssMatchedTitle(factCheck.getRssMatchedTitle())
                .rssMatchSource(factCheck.getRssMatchSource())
                .factClaimsFound(factCheck.getClaimsFound())
                .factCheckClaims(factCheck.getClaims())
                .factCheckVerdict(factCheck.getVerdict())
                .sentimentScore(sentiment.score())
                .sentimentLabel(sentiment.label())
                .sentimentRawSum(sentiment.rawSum())
                .sentimentWordsMatched(sentiment.wordsMatched())
                .readabilityScore(readability.fleschScore())
                .readabilityLevel(readability.level())
                .wordCount(readability.wordCount())
                .avgWordsPerSentence(readability.avgWordsPerSentence())
                .avgSyllablesPerWord(readability.avgSyllablesPerWord())
                .reasoning(buildReasoning(linguistic, crossSource, factCheck))
                .build();

        return analysisRepository.save(analysis);
    }

    private String buildReasoning(LinguisticResult linguistic,
                                   CrossSourceResult crossSource,
                                   FactCheckLayerResult factCheck) {
        List<String> notes = new ArrayList<>();

        // Шар 1: лінгвістика
        if (!linguistic.getHedgeWordsFound().isEmpty()) {
            notes.add("Uses indirect speech markers: " +
                    String.join(", ", linguistic.getHedgeWordsFound()));
        }
        if (!linguistic.getClickbaitIndicators().isEmpty()) {
            notes.add("Title contains clickbait patterns: " +
                    String.join(", ", linguistic.getClickbaitIndicators()));
        }
        if (!linguistic.getEmotionalWordsFound().isEmpty()) {
            notes.add("Contains emotionally charged language: " +
                    String.join(", ", linguistic.getEmotionalWordsFound()));
        }
        if (!linguistic.getConspiracyIndicators().isEmpty()) {
            notes.add("Conspiracy patterns detected (cross-source score overridden to 0, score capped at 40): " +
                    String.join(", ", linguistic.getConspiracyIndicators()));
        }

        // Шар 2: крос-перевірка
        if (crossSource.getSourcesConfirmed() == 3) {
            notes.add("Topic confirmed by all 3 sources (NewsAPI, Guardian, GNews)");
        } else if (crossSource.getSourcesConfirmed() > 0) {
            notes.add("Topic confirmed by " + crossSource.getSourcesConfirmed() + "/3 sources: " +
                    String.join(", ", crossSource.getConfirmedSources()));
        } else if (!crossSource.getKeywordsUsed().isBlank()) {
            notes.add("Topic not found in any cross-source search (keywords: \"" +
                    crossSource.getKeywordsUsed() + "\")");
        }

        // Шар 3: fact check
        if (factCheck.getClaimsFound() > 0) {
            notes.add(factCheck.getVerdict());
        }

        if (notes.isEmpty()) {
            return "No significant credibility issues detected.";
        }
        return String.join(". ", notes) + ".";
    }
}
