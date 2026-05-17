package com.lake_team.fistserios.analysis;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FactCheckLayerResult {
    private final int score;                   // 0–30 (сума підшарів)

    // Підшар 1: репутація джерела (0–12)
    private final int sourceReputationScore;
    private final String sourceDomain;
    private final String sourceReputationTier; // HIGH / GOOD / UNKNOWN / LOW

    // Підшар 2: ClaimBuster (0–10)
    private final int claimBusterScore;
    private final double claimBusterRaw;       // 0.0–1.0, -1 якщо недоступний

    // Підшар 3: RSS fact-check (0–8)
    private final int rssCheckScore;
    private final String rssMatchedTitle;      // null якщо не знайдено
    private final String rssMatchSource;       // "Snopes" / "PolitiFact" / null

    // Загальний висновок
    private final int claimsFound;
    private final List<FactCheckClaim> claims;
    private final String verdict;
}
