package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FactCheckAnalyzer {

    private final SourceReputationChecker reputationChecker;
    private final ClaimBusterChecker      claimBusterChecker;
    private final FactCheckRssChecker     rssChecker;
    private final RecycledNewsDetector    recycledNewsDetector;

    public FactCheckLayerResult analyze(NewsItem item) {
        return analyze(item, AnalysisOptions.FactCheckSubOptions.all());
    }

    public FactCheckLayerResult analyze(NewsItem item, AnalysisOptions.FactCheckSubOptions sub) {

        SourceReputationChecker.ReputationResult reputation = sub.sourceReputation()
                ? reputationChecker.check(item)
                : new SourceReputationChecker.ReputationResult(0, "SKIPPED", extractDomain(item));

        SourceReputationChecker.UrlAnalysisResult urlAnalysis = sub.extendedUrlAnalysis()
                ? reputationChecker.checkUrl(item.getUrl())
                : new SourceReputationChecker.UrlAnalysisResult("SKIPPED", List.of(), 0);

        ClaimBusterChecker.ClaimBusterResult claimBuster = sub.claimBuster()
                ? claimBusterChecker.check(item)
                : new ClaimBusterChecker.ClaimBusterResult(0, 0.0);

        FactCheckRssChecker.RssCheckResult rss = sub.rssCheck()
                ? rssChecker.check(item)
                : new FactCheckRssChecker.RssCheckResult(0, null, null);

        RecycledNewsDetector.RecycledResult recycled = sub.recycledNews()
                ? recycledNewsDetector.detect(item)
                : new RecycledNewsDetector.RecycledResult(false, null, 0);

        int total = reputation.score()
                  + claimBuster.score()
                  + rss.score()
                  + urlAnalysis.penalty()
                  + recycled.penalty();
        total = Math.max(total, 0);

        log.debug("FactCheck [{}]: rep={} url={} cb={} rss={} recycled={} → {}",
                item.getTitle(), reputation.score(), urlAnalysis.penalty(),
                claimBuster.score(), rss.score(), recycled.penalty(), total);

        return FactCheckLayerResult.builder()
                .score(total)
                .sourceReputationScore(reputation.score())
                .sourceDomain(reputation.domain())
                .sourceReputationTier(reputation.tier())
                .urlAnalysisTier(urlAnalysis.tier())
                .urlWarnings(urlAnalysis.warnings())
                .urlPenalty(urlAnalysis.penalty())
                .claimBusterScore(claimBuster.score())
                .claimBusterRaw(claimBuster.rawScore())
                .rssCheckScore(rss.score())
                .rssMatchedTitle(rss.matchedTitle())
                .rssMatchSource(rss.source())
                .recycledNewsDetected(recycled.detected())
                .recycledNewsReason(recycled.reason())
                .recycledNewsPenalty(recycled.penalty())
                .claimsFound(rss.matchedTitle() != null ? 1 : 0)
                .claims(List.of())
                .verdict(buildVerdict(reputation, urlAnalysis, claimBuster, rss, recycled, sub))
                .build();
    }

    private String extractDomain(NewsItem item) {
        if (item.getUrl() == null) return "unknown";
        try {
            URI uri = new URI(item.getUrl());
            String host = uri.getHost();
            return host != null ? host.replaceFirst("^www\\.", "") : "unknown";
        } catch (Exception e) { return "unknown"; }
    }

    private String buildVerdict(SourceReputationChecker.ReputationResult reputation,
                                SourceReputationChecker.UrlAnalysisResult urlAnalysis,
                                ClaimBusterChecker.ClaimBusterResult claimBuster,
                                FactCheckRssChecker.RssCheckResult rss,
                                RecycledNewsDetector.RecycledResult recycled,
                                AnalysisOptions.FactCheckSubOptions sub) {
        StringBuilder sb = new StringBuilder();

        if (sub.sourceReputation()) {
            sb.append("Source [").append(reputation.domain()).append("]: ")
              .append(reputation.tier()).append(". ");
        }

        if (sub.extendedUrlAnalysis() && !"SKIPPED".equals(urlAnalysis.tier())) {
            if (!"CLEAN".equals(urlAnalysis.tier())) {
                sb.append("URL flags: ").append(String.join("; ", urlAnalysis.warnings()))
                  .append(" (").append(urlAnalysis.tier()).append("). ");
            }
        }

        if (sub.claimBuster() && claimBuster.rawScore() >= 0) {
            sb.append("Claim-worthiness: ")
              .append(String.format("%.2f", claimBuster.rawScore())).append(". ");
        }

        if (sub.rssCheck()) {
            if (rss.matchedTitle() != null) {
                sb.append("Found in ").append(rss.source()).append(" fact-checks: \"")
                  .append(truncate(rss.matchedTitle(), 80)).append("\". ");
            } else {
                sb.append("Not found in Snopes/PolitiFact fact-checks. ");
            }
        }

        if (sub.recycledNews() && recycled.detected()) {
            sb.append("Recycled news detected: ").append(recycled.reason()).append(". ");
        }

        return sb.length() == 0 ? "All fact-check sub-methods skipped." : sb.toString().trim();
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
