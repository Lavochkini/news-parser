package com.lake_team.fistserios.analysis;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CrossSourceResult {
    private final int score;             // 0–35
    private final int sourcesConfirmed;  // 0–3
    private final List<String> confirmedSources;
    private final String keywordsUsed;
}
