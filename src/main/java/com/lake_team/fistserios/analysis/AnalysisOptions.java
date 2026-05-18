package com.lake_team.fistserios.analysis;

public record AnalysisOptions(
        boolean runLinguistic,
        boolean runCrossSource,
        boolean runFactCheck,
        boolean runSentiment,
        boolean runReadability
) {
    public static AnalysisOptions all() {
        return new AnalysisOptions(true, true, true, true, true);
    }
}
