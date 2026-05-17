package com.lake_team.fistserios.analysis;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LinguisticResult {
    private final int score;                    // 0–35
    private final List<String> hedgeWordsFound;
    private final List<String> clickbaitIndicators;
    private final List<String> emotionalWordsFound;
    private final List<String> conspiracyIndicators;
}
