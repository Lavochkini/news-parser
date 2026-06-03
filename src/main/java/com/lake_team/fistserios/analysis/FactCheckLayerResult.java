package com.lake_team.fistserios.analysis;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FactCheckLayerResult {

    private final int score;                    // підсумок усіх підшарів (≥0)

    // ── Підшар 1: репутація домену (0–12) ──
    private final int sourceReputationScore;
    private final String sourceDomain;
    private final String sourceReputationTier;  // HIGH / GOOD / UNKNOWN / LOW

    // ── Підшар 1b: розширений URL-аналіз (penalty 0..−5) ──
    private final String urlAnalysisTier;       // CLEAN / SUSPICIOUS / FAKE
    private final List<String> urlWarnings;     // конкретні причини
    private final int urlPenalty;               // 0, −2, −4, або −5

    // ── Підшар 2: ClaimBuster (0–10) ──
    private final int claimBusterScore;
    private final double claimBusterRaw;

    // ── Підшар 3: RSS fact-check (0–8) ──
    private final int rssCheckScore;
    private final String rssMatchedTitle;
    private final String rssMatchSource;

    // ── Підшар 4: перевидана новина (penalty 0..−5) ──
    private final boolean recycledNewsDetected;
    private final String  recycledNewsReason;
    private final int     recycledNewsPenalty;  // 0 або −5

    // ── Загальне ──
    private final int claimsFound;
    private final List<FactCheckClaim> claims;
    private final String verdict;
}
