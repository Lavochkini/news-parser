package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.model.NewsSourceType;
import com.lake_team.fistserios.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsAnalysisService {

    private final NewsAnalysisRepository  analysisRepository;
    private final NewsRepository           newsRepository;
    private final LinguisticAnalyzer       linguisticAnalyzer;
    private final CrossSourceAnalyzer      crossSourceAnalyzer;
    private final FactCheckAnalyzer        factCheckAnalyzer;
    private final SentimentAnalyzer        sentimentAnalyzer;
    private final ReadabilityAnalyzer      readabilityAnalyzer;
    private final WikipediaEntityChecker   wikipediaChecker;
    private final LanguageDetector         languageDetector;

    public NewsAnalysis analyzeArticle(String newsItemId) {
        return analysisRepository.findByNewsItemId(newsItemId)
                .orElseGet(() -> runAnalysis(newsItemId));
    }

    public NewsAnalysis reanalyzeArticle(String newsItemId) {
        analysisRepository.findByNewsItemId(newsItemId)
                .ifPresent(a -> analysisRepository.deleteById(a.getId()));
        return runAnalysis(newsItemId);
    }

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

    public BatchResult analyzeDatasetBatch(int limit) {
        List<com.lake_team.fistserios.model.NewsItem> items =
                newsRepository.findBySourceType(com.lake_team.fistserios.model.NewsSourceType.DATASET);

        int processed = 0, skipped = 0, failed = 0;
        for (com.lake_team.fistserios.model.NewsItem item : items) {
            if (processed + skipped + failed >= limit) break;
            if (analysisRepository.existsByNewsItemId(item.getId())) {
                skipped++;
                continue;
            }
            try {
                runAnalysis(item.getId());
                processed++;
                log.info("Dataset batch [{}/{}]: analyzed {}", processed, limit, item.getId());
            } catch (Exception e) {
                failed++;
                log.warn("Dataset batch: failed {}: {}", item.getId(), e.getMessage());
            }
        }
        log.info("Dataset batch done: processed={}, skipped={}, failed={}", processed, skipped, failed);
        return new BatchResult(processed, skipped, failed);
    }

    public record BatchResult(int processed, int skipped, int failed) {}

    public PartialBatchResult analyzePartialBatch(PartialBatchOptions opts) {
        List<NewsItem> candidates;
        if (opts.sourceType() != null && !opts.sourceType().isBlank()) {
            try {
                NewsSourceType st = NewsSourceType.valueOf(opts.sourceType().toUpperCase());
                candidates = newsRepository.findBySourceType(st);
            } catch (IllegalArgumentException e) {
                candidates = newsRepository.findAll();
            }
        } else {
            candidates = newsRepository.findAll();
        }

        LocalDateTime from = parseDate(opts.dateFrom(), false);
        LocalDateTime to   = parseDate(opts.dateTo(),   true);

        Stream<NewsItem> stream = candidates.stream();

        if (opts.category() != null && !opts.category().isBlank()) {
            final String cat = opts.category().trim().toLowerCase();
            stream = stream.filter(n -> n.getCategory() != null && n.getCategory().equalsIgnoreCase(cat));
        }
        if (from != null) {
            final LocalDateTime f = from;
            stream = stream.filter(n -> n.getPublishedAt() != null && !n.getPublishedAt().isBefore(f));
        }
        if (to != null) {
            final LocalDateTime t = to;
            stream = stream.filter(n -> n.getPublishedAt() != null && !n.getPublishedAt().isAfter(t));
        }

        stream = stream.sorted(Comparator.comparing(
                NewsItem::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        int effectiveLimit = (opts.limit() != null && opts.limit() > 0) ? opts.limit() : Integer.MAX_VALUE;
        List<NewsItem> items = stream.limit(effectiveLimit).collect(Collectors.toList());

        int total = items.size(), processed = 0, skipped = 0, failed = 0;

        for (NewsItem item : items) {
            boolean exists = analysisRepository.existsByNewsItemId(item.getId());
            if (exists && !opts.forceReanalyze()) {
                skipped++;
                continue;
            }
            if (exists) {
                analysisRepository.findByNewsItemId(item.getId())
                        .ifPresent(a -> analysisRepository.deleteById(a.getId()));
            }
            try {
                runAnalysis(item.getId());
                processed++;
            } catch (Exception e) {
                failed++;
                log.warn("Partial batch: failed [{}]: {}", item.getId(), e.getMessage());
            }
        }

        log.info("Partial batch done: total={} processed={} skipped={} failed={}", total, processed, skipped, failed);
        return new PartialBatchResult(total, processed, skipped, failed);
    }

    public record PartialBatchResult(int total, int processed, int skipped, int failed) {}

    private LocalDateTime parseDate(String s, boolean endOfDay) {
        if (s == null || s.isBlank()) return null;
        try {
            LocalDate d = LocalDate.parse(s.trim());
            return endOfDay ? d.atTime(23, 59, 59) : d.atStartOfDay();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public Optional<NewsAnalysis> getByNewsItemId(String newsItemId) {
        return analysisRepository.findByNewsItemId(newsItemId);
    }

    public List<NewsAnalysis> getByUsername(String username) {
        return analysisRepository.findAllByAnalyzedByUsernameOrderByAnalyzedAtDesc(username);
    }

    public NewsAnalysis analyzeManual(String newsItemId, String username, AnalysisOptions options) {
        analysisRepository.findByNewsItemId(newsItemId)
                .ifPresent(a -> analysisRepository.deleteById(a.getId()));
        return runAnalysis(newsItemId, username, options.withDefaults());
    }

    @Async
    public CompletableFuture<Void> forceReanalyzeAllAsync() {
        log.info("Force batch: clearing all analyses...");
        analysisRepository.deleteAll();
        return analyzeAllAsync();
    }

    private NewsAnalysis runAnalysis(String newsItemId) {
        return runAnalysis(newsItemId, null, AnalysisOptions.all());
    }

    private NewsAnalysis runAnalysis(String newsItemId, String username, AnalysisOptions opts) {
        opts = opts.withDefaults();

        NewsItem item = newsRepository.findById(newsItemId)
                .orElseThrow(() -> new IllegalArgumentException("NewsItem not found: " + newsItemId));

        LanguageDetector.Language lang = languageDetector.detect(item);
        if (!languageDetector.isEnglishCompatible(lang)) {
            opts = adjustForLanguage(opts, lang);
            log.debug("Language detected: {} for [{}] — adjusted options", lang, item.getTitle());
        }

        LinguisticResult linguistic = opts.runLinguistic()
                ? linguisticAnalyzer.analyze(item, opts.linguistic())
                : emptyLinguistic();

        boolean isDataset = NewsSourceType.DATASET.equals(item.getSourceType());
        CrossSourceResult crossSource;
        if (!opts.runCrossSource() || isDataset) {
            crossSource = CrossSourceResult.builder().score(0).sourcesConfirmed(0)
                    .confirmedSources(List.of())
                    .keywordsUsed(isDataset ? "skipped-dataset" : "skipped-by-user").build();
        } else {
            crossSource = crossSourceAnalyzer.analyze(item);
        }

        FactCheckLayerResult factCheck = opts.runFactCheck()
                ? factCheckAnalyzer.analyze(item, opts.factCheck())
                : emptyFactCheck();

        SentimentAnalyzer.SentimentResult sentiment = opts.runSentiment()
                ? sentimentAnalyzer.analyze(item)
                : new SentimentAnalyzer.SentimentResult(0.0, "N/A", 0, 0);
        ReadabilityAnalyzer.ReadabilityResult readability = opts.runReadability()
                ? readabilityAnalyzer.analyze(item)
                : new ReadabilityAnalyzer.ReadabilityResult(0, "N/A", 0, 0, 0.0, 0.0);

        WikipediaEntityChecker.WikiResult wikiResult = opts.runWikipedia()
                ? wikipediaChecker.check(item)
                : new WikipediaEntityChecker.WikiResult(List.of(), List.of(), 0);

        boolean conspiracyDetected = !linguistic.getConspiracyIndicators().isEmpty();
        int effectiveCrossSourceScore = conspiracyDetected ? 0 : crossSource.getScore();
        int rawScore = linguistic.getScore() + effectiveCrossSourceScore + factCheck.getScore();
        int credibilityScore = conspiracyDetected ? Math.min(rawScore, 40) : rawScore;

        NewsAnalysis analysis = NewsAnalysis.builder()
                .newsItemId(newsItemId)
                .analyzedByUsername(username)
                .isManual(username != null)
                .credibilityScore(credibilityScore)
                .linguisticScore(linguistic.getScore())
                .hedgeWordsFound(linguistic.getHedgeWordsFound())
                .clickbaitIndicators(linguistic.getClickbaitIndicators())
                .emotionalWordsFound(linguistic.getEmotionalWordsFound())
                .manipulationIndicators(linguistic.getManipulationIndicators())
                .conspiracyIndicators(linguistic.getConspiracyIndicators())
                .anonymousSourcesFound(linguistic.getAnonymousSourcesFound())
                .citationCount(linguistic.getCitationCount())
                .headlineBodyOverlap(linguistic.getHeadlineBodyOverlap())
                .betteridgeDetected(linguistic.isBetteridgeDetected())
                .crossSourceScore(effectiveCrossSourceScore)
                .sourcesConfirmed(crossSource.getSourcesConfirmed())
                .confirmedSources(crossSource.getConfirmedSources())
                .keywordsUsed(crossSource.getKeywordsUsed())
                .factCheckScore(factCheck.getScore())
                .sourceReputationScore(factCheck.getSourceReputationScore())
                .sourceDomain(factCheck.getSourceDomain())
                .sourceReputationTier(factCheck.getSourceReputationTier())
                .urlAnalysisTier(factCheck.getUrlAnalysisTier())
                .urlWarnings(factCheck.getUrlWarnings())
                .urlPenalty(factCheck.getUrlPenalty())
                .claimBusterScore(factCheck.getClaimBusterScore())
                .claimBusterRaw(factCheck.getClaimBusterRaw())
                .rssCheckScore(factCheck.getRssCheckScore())
                .rssMatchedTitle(factCheck.getRssMatchedTitle())
                .rssMatchSource(factCheck.getRssMatchSource())
                .recycledNewsDetected(factCheck.isRecycledNewsDetected())
                .recycledNewsReason(factCheck.getRecycledNewsReason())
                .recycledNewsPenalty(factCheck.getRecycledNewsPenalty())
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
                .wikiEntitiesChecked(wikiResult.checked())
                .wikiEntitiesFound(wikiResult.found())
                .wikiEntitiesTotal(wikiResult.total())
                .detectedLanguage(lang.name())
                .languageNote(languageDetector.buildNote(lang))
                .reasoning(buildReasoning(linguistic, crossSource, factCheck))
                .build();

        return analysisRepository.save(analysis);
    }

    private String buildReasoning(LinguisticResult linguistic,
                                   CrossSourceResult crossSource,
                                   FactCheckLayerResult factCheck) {
        List<String> notes = new ArrayList<>();

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
        if (!linguistic.getManipulationIndicators().isEmpty()) {
            notes.add("Political manipulation patterns detected: " +
                    String.join(", ", linguistic.getManipulationIndicators()));
        }
        if (!linguistic.getConspiracyIndicators().isEmpty()) {
            notes.add("Conspiracy patterns detected (cross-source score overridden to 0, score capped at 40): " +
                    String.join(", ", linguistic.getConspiracyIndicators()));
        }
        if (linguistic.getAnonymousSourcesFound() != null && !linguistic.getAnonymousSourcesFound().isEmpty()) {
            notes.add("Anonymous attribution detected: " +
                    String.join("; ", linguistic.getAnonymousSourcesFound()));
        }
        if (linguistic.getCitationCount() == 0) {
            notes.add("No explicit citations found in the article");
        }
        if (linguistic.getHeadlineBodyOverlap() < 0.20) {
            notes.add(String.format("Low headline–body overlap (%.0f%%)", linguistic.getHeadlineBodyOverlap() * 100));
        }
        if (linguistic.isBetteridgeDetected()) {
            notes.add("Headline is a question (Betteridge's Law: the answer is likely 'No')");
        }

        if (crossSource.getSourcesConfirmed() == 3) {
            notes.add("Topic confirmed by all 3 sources (NewsAPI, Guardian, GNews)");
        } else if (crossSource.getSourcesConfirmed() > 0) {
            notes.add("Topic confirmed by " + crossSource.getSourcesConfirmed() + "/3 sources: " +
                    String.join(", ", crossSource.getConfirmedSources()));
        } else if (!crossSource.getKeywordsUsed().isBlank()) {
            notes.add("Topic not found in any cross-source search (keywords: \"" +
                    crossSource.getKeywordsUsed() + "\")");
        }

        if (factCheck.getClaimsFound() > 0) {
            notes.add(factCheck.getVerdict());
        }

        if (notes.isEmpty()) {
            return "No significant credibility issues detected.";
        }
        return String.join(". ", notes) + ".";
    }

    private AnalysisOptions adjustForLanguage(AnalysisOptions opts, LanguageDetector.Language lang) {
        var fc = opts.factCheck() != null ? opts.factCheck() : AnalysisOptions.FactCheckSubOptions.all();
        var adjustedFc = new AnalysisOptions.FactCheckSubOptions(
                fc.sourceReputation(),
                fc.claimBuster(),
                false,
                fc.extendedUrlAnalysis(),
                fc.recycledNews()
        );
        return new AnalysisOptions(
                opts.runLinguistic(),
                opts.linguistic(),
                opts.runCrossSource(),
                opts.runFactCheck(),
                adjustedFc,
                false,
                false,
                opts.runWikipedia()
        );
    }

    private LinguisticResult emptyLinguistic() {
        return LinguisticResult.builder()
                .score(0)
                .hedgeWordsFound(List.of())
                .clickbaitIndicators(List.of())
                .emotionalWordsFound(List.of())
                .manipulationIndicators(List.of())
                .conspiracyIndicators(List.of())
                .anonymousSourcesFound(List.of())
                .citationCount(0)
                .headlineBodyOverlap(1.0)
                .betteridgeDetected(false)
                .build();
    }

    private FactCheckLayerResult emptyFactCheck() {
        return FactCheckLayerResult.builder()
                .score(0)
                .sourceReputationScore(0).sourceDomain("").sourceReputationTier("N/A")
                .urlAnalysisTier("SKIPPED").urlWarnings(List.of()).urlPenalty(0)
                .claimBusterScore(0).claimBusterRaw(0)
                .rssCheckScore(0).rssMatchedTitle(null).rssMatchSource(null)
                .recycledNewsDetected(false).recycledNewsReason(null).recycledNewsPenalty(0)
                .claimsFound(0).claims(List.of()).verdict("Fact-check skipped")
                .build();
    }
}
