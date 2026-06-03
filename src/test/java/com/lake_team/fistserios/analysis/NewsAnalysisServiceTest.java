package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.model.NewsSourceType;
import com.lake_team.fistserios.repository.NewsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NewsAnalysisService")
class NewsAnalysisServiceTest {

    @Mock private NewsAnalysisRepository  analysisRepository;
    @Mock private NewsRepository           newsRepository;
    @Mock private LinguisticAnalyzer       linguisticAnalyzer;
    @Mock private CrossSourceAnalyzer      crossSourceAnalyzer;
    @Mock private FactCheckAnalyzer        factCheckAnalyzer;
    @Mock private SentimentAnalyzer        sentimentAnalyzer;
    @Mock private ReadabilityAnalyzer      readabilityAnalyzer;
    @Mock private WikipediaEntityChecker   wikipediaChecker;
    @Mock private LanguageDetector         languageDetector;

    @InjectMocks
    private NewsAnalysisService service;

    private NewsItem englishItem(String id) {
        return NewsItem.builder()
                .id(id)
                .title("Economy grows faster than expected")
                .description("GDP increased by 3.2 percent")
                .fullContent("The economy showed strong growth according to officials.")
                .url("https://reuters.com/article/" + id)
                .sourceType(NewsSourceType.NEWS_API)
                .build();
    }

    private LinguisticResult emptyLinguistic() {
        return LinguisticResult.builder()
                .score(35)
                .hedgeWordsFound(List.of())
                .clickbaitIndicators(List.of())
                .emotionalWordsFound(List.of())
                .manipulationIndicators(List.of())
                .conspiracyIndicators(List.of())
                .anonymousSourcesFound(List.of())
                .citationCount(3)
                .headlineBodyOverlap(0.8)
                .betteridgeDetected(false)
                .build();
    }

    private CrossSourceResult crossResult(int score) {
        return CrossSourceResult.builder()
                .score(score)
                .sourcesConfirmed(0)
                .confirmedSources(List.of())
                .keywordsUsed("economy growth")
                .build();
    }

    private FactCheckLayerResult emptyFactCheck(int score) {
        return FactCheckLayerResult.builder()
                .score(score)
                .sourceReputationScore(12)
                .sourceDomain("reuters.com")
                .sourceReputationTier("HIGH")
                .urlAnalysisTier("CLEAN")
                .urlWarnings(List.of())
                .urlPenalty(0)
                .claimBusterScore(6)
                .claimBusterRaw(0.5)
                .rssCheckScore(6)
                .rssMatchedTitle(null)
                .rssMatchSource(null)
                .recycledNewsDetected(false)
                .recycledNewsReason(null)
                .recycledNewsPenalty(0)
                .claimsFound(0)
                .claims(List.of())
                .verdict("Source: HIGH.")
                .build();
    }


    @Test
    @DisplayName("analyzeArticle: існуючий аналіз → повертає кешований, без повторного аналізу")
    void analyzeArticle_existingResult_returnsCached() {
        String newsId = "news-001";
        NewsAnalysis cached = NewsAnalysis.builder()
                .newsItemId(newsId)
                .credibilityScore(80)
                .build();
        when(analysisRepository.findByNewsItemId(newsId)).thenReturn(Optional.of(cached));

        NewsAnalysis result = service.analyzeArticle(newsId);

        assertThat(result.getCredibilityScore()).isEqualTo(80);
        verify(newsRepository, never()).findById(any());
        verify(linguisticAnalyzer, never()).analyze(any(), any());
    }

    @Test
    @DisplayName("analyzeArticle: аналізу немає → запускає повний аналіз")
    void analyzeArticle_noExisting_runsAnalysis() {
        String newsId = "news-002";
        NewsItem item = englishItem(newsId);

        when(analysisRepository.findByNewsItemId(newsId)).thenReturn(Optional.empty());
        when(newsRepository.findById(newsId)).thenReturn(Optional.of(item));
        when(languageDetector.detect(item)).thenReturn(LanguageDetector.Language.ENGLISH);
        when(languageDetector.isEnglishCompatible(LanguageDetector.Language.ENGLISH)).thenReturn(true);
        when(languageDetector.buildNote(any())).thenReturn(null);
        when(linguisticAnalyzer.analyze(eq(item), any())).thenReturn(emptyLinguistic());
        when(crossSourceAnalyzer.analyze(item)).thenReturn(crossResult(20));
        when(factCheckAnalyzer.analyze(eq(item), any())).thenReturn(emptyFactCheck(24));
        when(sentimentAnalyzer.analyze(item))
                .thenReturn(new SentimentAnalyzer.SentimentResult(0.2, "POSITIVE", 10, 5));
        when(readabilityAnalyzer.analyze(item))
                .thenReturn(new ReadabilityAnalyzer.ReadabilityResult(65.0, "STANDARD", 200, 15, 15.0, 1.5));
        when(wikipediaChecker.check(item))
                .thenReturn(new WikipediaEntityChecker.WikiResult(List.of(), List.of(), 0));
        when(analysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NewsAnalysis result = service.analyzeArticle(newsId);

        verify(linguisticAnalyzer).analyze(eq(item), any());
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("analyzeManual: видаляє попередній результат перед новим аналізом")
    void analyzeManual_deletesExisting_beforeReanalysis() {
        String newsId = "news-003";
        NewsItem item = englishItem(newsId);
        NewsAnalysis old = NewsAnalysis.builder().id("old-id").newsItemId(newsId).build();

        when(analysisRepository.findByNewsItemId(newsId)).thenReturn(Optional.of(old));
        when(newsRepository.findById(newsId)).thenReturn(Optional.of(item));
        when(languageDetector.detect(item)).thenReturn(LanguageDetector.Language.ENGLISH);
        when(languageDetector.isEnglishCompatible(LanguageDetector.Language.ENGLISH)).thenReturn(true);
        when(languageDetector.buildNote(any())).thenReturn(null);
        when(linguisticAnalyzer.analyze(eq(item), any())).thenReturn(emptyLinguistic());
        when(crossSourceAnalyzer.analyze(item)).thenReturn(crossResult(20));
        when(factCheckAnalyzer.analyze(eq(item), any())).thenReturn(emptyFactCheck(24));
        when(sentimentAnalyzer.analyze(item))
                .thenReturn(new SentimentAnalyzer.SentimentResult(0.0, "NEUTRAL", 0, 0));
        when(readabilityAnalyzer.analyze(item))
                .thenReturn(new ReadabilityAnalyzer.ReadabilityResult(60.0, "STANDARD", 100, 12, 12.0, 1.4));
        when(wikipediaChecker.check(item))
                .thenReturn(new WikipediaEntityChecker.WikiResult(List.of(), List.of(), 0));
        when(analysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.analyzeManual(newsId, "user1", AnalysisOptions.all());

        verify(analysisRepository).deleteById("old-id");
    }

    @Test
    @DisplayName("Українська мова → sentiment і readability НЕ викликаються")
    void ukrainianLanguage_skipsSentimentAndReadability() {
        String newsId = "news-ua-001";
        NewsItem item = NewsItem.builder()
                .id(newsId)
                .title("Президент підписав закон")
                .description("Важливе рішення для України")
                .url("https://example.com/" + newsId)
                .sourceType(NewsSourceType.NEWS_API)
                .build();

        when(analysisRepository.findByNewsItemId(newsId)).thenReturn(Optional.empty());
        when(newsRepository.findById(newsId)).thenReturn(Optional.of(item));
        when(languageDetector.detect(item)).thenReturn(LanguageDetector.Language.UKRAINIAN);
        when(languageDetector.isEnglishCompatible(LanguageDetector.Language.UKRAINIAN)).thenReturn(false);
        when(languageDetector.buildNote(LanguageDetector.Language.UKRAINIAN))
                .thenReturn("Мова: українська. Вимкнено: тональність, читабельність, RSS.");
        when(linguisticAnalyzer.analyze(eq(item), any())).thenReturn(emptyLinguistic());
        when(crossSourceAnalyzer.analyze(item)).thenReturn(crossResult(15));
        when(factCheckAnalyzer.analyze(eq(item), any())).thenReturn(emptyFactCheck(20));
        when(wikipediaChecker.check(item))
                .thenReturn(new WikipediaEntityChecker.WikiResult(List.of(), List.of(), 0));
        when(analysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.analyzeArticle(newsId);

        verify(sentimentAnalyzer, never()).analyze(any());
        verify(readabilityAnalyzer, never()).analyze(any());
    }

    @Test
    @DisplayName("Українська мова → збережений результат містить languageNote")
    void ukrainianLanguage_savesLanguageNote() {
        String newsId = "news-ua-002";
        NewsItem item = NewsItem.builder()
                .id(newsId)
                .title("Президент їде до Брюсселя")
                .url("https://example.com/" + newsId)
                .sourceType(NewsSourceType.NEWS_API)
                .build();

        when(analysisRepository.findByNewsItemId(newsId)).thenReturn(Optional.empty());
        when(newsRepository.findById(newsId)).thenReturn(Optional.of(item));
        when(languageDetector.detect(item)).thenReturn(LanguageDetector.Language.UKRAINIAN);
        when(languageDetector.isEnglishCompatible(LanguageDetector.Language.UKRAINIAN)).thenReturn(false);
        when(languageDetector.buildNote(LanguageDetector.Language.UKRAINIAN)).thenReturn("Мова: українська.");
        when(linguisticAnalyzer.analyze(eq(item), any())).thenReturn(emptyLinguistic());
        when(crossSourceAnalyzer.analyze(item)).thenReturn(crossResult(10));
        when(factCheckAnalyzer.analyze(eq(item), any())).thenReturn(emptyFactCheck(15));
        when(wikipediaChecker.check(item))
                .thenReturn(new WikipediaEntityChecker.WikiResult(List.of(), List.of(), 0));

        ArgumentCaptor<NewsAnalysis> captor = ArgumentCaptor.forClass(NewsAnalysis.class);
        when(analysisRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.analyzeArticle(newsId);

        assertThat(captor.getValue().getLanguageNote()).contains("українська");
        assertThat(captor.getValue().getDetectedLanguage()).isEqualTo("UKRAINIAN");
    }


    @Test
    @DisplayName("Конспірологія виявлена → загальний score ≤ 40")
    void conspiracyDetected_scoreCappedAt40() {
        String newsId = "news-conspiracy";
        NewsItem item = englishItem(newsId);

        LinguisticResult conspiracyResult = LinguisticResult.builder()
                .score(15)
                .hedgeWordsFound(List.of())
                .clickbaitIndicators(List.of())
                .emotionalWordsFound(List.of())
                .manipulationIndicators(List.of())
                .conspiracyIndicators(List.of("deep state (plot|agenda|conspiracy)"))
                .anonymousSourcesFound(List.of())
                .citationCount(0)
                .headlineBodyOverlap(0.5)
                .betteridgeDetected(false)
                .build();

        when(analysisRepository.findByNewsItemId(newsId)).thenReturn(Optional.empty());
        when(newsRepository.findById(newsId)).thenReturn(Optional.of(item));
        when(languageDetector.detect(item)).thenReturn(LanguageDetector.Language.ENGLISH);
        when(languageDetector.isEnglishCompatible(any())).thenReturn(true);
        when(languageDetector.buildNote(any())).thenReturn(null);
        when(linguisticAnalyzer.analyze(eq(item), any())).thenReturn(conspiracyResult);
        when(crossSourceAnalyzer.analyze(item)).thenReturn(
                CrossSourceResult.builder().score(20).sourcesConfirmed(1)
                        .confirmedSources(List.of()).keywordsUsed("test").build());
        when(factCheckAnalyzer.analyze(eq(item), any())).thenReturn(emptyFactCheck(30));
        when(sentimentAnalyzer.analyze(item))
                .thenReturn(new SentimentAnalyzer.SentimentResult(0.0, "NEUTRAL", 0, 0));
        when(readabilityAnalyzer.analyze(item))
                .thenReturn(new ReadabilityAnalyzer.ReadabilityResult(55.0, "STANDARD", 100, 12, 12.0, 1.4));
        when(wikipediaChecker.check(item))
                .thenReturn(new WikipediaEntityChecker.WikiResult(List.of(), List.of(), 0));
        when(analysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<NewsAnalysis> captor = ArgumentCaptor.forClass(NewsAnalysis.class);
        when(analysisRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.analyzeArticle(newsId);
        assertThat(captor.getValue().getCredibilityScore()).isLessThanOrEqualTo(40);
    }
}
