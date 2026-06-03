package com.lake_team.fistserios.analysis;

public record AnalysisOptions(
        boolean runLinguistic,
        LinguisticSubOptions linguistic,
        boolean runCrossSource,
        boolean runFactCheck,
        FactCheckSubOptions factCheck,
        boolean runSentiment,
        boolean runReadability,
        boolean runWikipedia        // додаткова метрика — Wikipedia верифікація
) {

    // ── Лінгвістичний шар ──────────────────────────────────────────────
    public record LinguisticSubOptions(
            boolean hedgeWords,
            boolean clickbait,
            boolean emotional,
            boolean manipulation,
            boolean conspiracy,
            boolean anonymousSources,   // "sources say", "insiders claim" без імені
            boolean citationDensity,    // кількість чітких атрибуцій
            boolean headlineConsistency,// перетин ключових слів заголовка і тіла
            boolean betteridge          // заголовок у формі питання
    ) {
        public static LinguisticSubOptions all() {
            return new LinguisticSubOptions(
                    true, true, true, true, true,
                    true, true, true, true);
        }
        public static LinguisticSubOptions none() {
            return new LinguisticSubOptions(
                    false, false, false, false, false,
                    false, false, false, false);
        }
    }

    // ── Факт-чек шар ───────────────────────────────────────────────────
    public record FactCheckSubOptions(
            boolean sourceReputation,
            boolean claimBuster,
            boolean rssCheck,
            boolean extendedUrlAnalysis, // підозрілі TLD, мімікрія домену
            boolean recycledNews         // стара новина перевидана як нова
    ) {
        public static FactCheckSubOptions all() {
            return new FactCheckSubOptions(true, true, true, true, true);
        }
        public static FactCheckSubOptions none() {
            return new FactCheckSubOptions(false, false, false, false, false);
        }
    }

    /** Усі шари і підметоди увімкнено */
    public static AnalysisOptions all() {
        return new AnalysisOptions(
                true, LinguisticSubOptions.all(),
                true,
                true, FactCheckSubOptions.all(),
                true, true, true);
    }

    /**
     * Якщо вкладені об'єкти null (старий JSON без sub-options) —
     * підставляємо all() для зворотньої сумісності.
     */
    public AnalysisOptions withDefaults() {
        return new AnalysisOptions(
                runLinguistic,
                linguistic != null ? linguistic : LinguisticSubOptions.all(),
                runCrossSource,
                runFactCheck,
                factCheck  != null ? factCheck  : FactCheckSubOptions.all(),
                runSentiment,
                runReadability,
                runWikipedia);
    }
}
