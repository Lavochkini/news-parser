package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Шар 3: комбінований fact-check з трьох підшарів.
 *
 *   sourceReputation (0–12) — довіра до домену
 *   claimBuster      (0–10) — check-worthiness тексту
 *   rssCheck         (0–8)  — пошук у Snopes / PolitiFact RSS
 *
 *   Разом max = 30
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FactCheckAnalyzer {

    private final SourceReputationChecker reputationChecker;
    private final ClaimBusterChecker      claimBusterChecker;
    private final FactCheckRssChecker     rssChecker;

    public FactCheckLayerResult analyze(NewsItem item) {
        SourceReputationChecker.ReputationResult reputation = reputationChecker.check(item);
        ClaimBusterChecker.ClaimBusterResult      claimBuster = claimBusterChecker.check(item);
        FactCheckRssChecker.RssCheckResult        rss = rssChecker.check(item);

        int total = reputation.score() + claimBuster.score() + rss.score();

        log.debug("FactCheck [{}]: reputation={} claimBuster={} rss={} → {}",
                item.getTitle(), reputation.score(), claimBuster.score(), rss.score(), total);

        return FactCheckLayerResult.builder()
                .score(total)
                .sourceReputationScore(reputation.score())
                .sourceDomain(reputation.domain())
                .sourceReputationTier(reputation.tier())
                .claimBusterScore(claimBuster.score())
                .claimBusterRaw(claimBuster.rawScore())
                .rssCheckScore(rss.score())
                .rssMatchedTitle(rss.matchedTitle())
                .rssMatchSource(rss.source())
                .claimsFound(rss.matchedTitle() != null ? 1 : 0)
                .claims(List.of())
                .verdict(buildVerdict(reputation, claimBuster, rss))
                .build();
    }

    private String buildVerdict(SourceReputationChecker.ReputationResult reputation,
                                ClaimBusterChecker.ClaimBusterResult claimBuster,
                                FactCheckRssChecker.RssCheckResult rss) {
        StringBuilder sb = new StringBuilder();

        sb.append("Source [").append(reputation.domain()).append("]: ")
          .append(reputation.tier()).append(". ");

        if (claimBuster.rawScore() >= 0) {
            sb.append("Claim-worthiness: ")
              .append(String.format("%.2f", claimBuster.rawScore())).append(". ");
        }

        if (rss.matchedTitle() != null) {
            sb.append("Found in ").append(rss.source()).append(" fact-checks: \"")
              .append(truncate(rss.matchedTitle(), 80)).append("\".");
        } else {
            sb.append("Not found in Snopes/PolitiFact fact-checks.");
        }

        return sb.toString();
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
