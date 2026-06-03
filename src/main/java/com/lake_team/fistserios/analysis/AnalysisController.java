package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
@CrossOrigin
public class AnalysisController {

    private final NewsAnalysisService analysisService;
    private final UserRepository userRepository;
    private final NewsAnalysisRepository analysisRepository;

    @GetMapping("/{newsItemId}/check")
    public ResponseEntity<Boolean> checkExists(@PathVariable String newsItemId) {
        return ResponseEntity.ok(analysisRepository.existsByNewsItemId(newsItemId));
    }

    @GetMapping("/{newsItemId}")
    public ResponseEntity<NewsAnalysis> getOrAnalyze(@PathVariable String newsItemId) {
        try {
            return ResponseEntity.ok(analysisService.analyzeArticle(newsItemId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{newsItemId}/reanalyze")
    public ResponseEntity<NewsAnalysis> reanalyze(@PathVariable String newsItemId) {
        try {
            return ResponseEntity.ok(analysisService.reanalyzeArticle(newsItemId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<String> batchAnalyze() {
        analysisService.analyzeAllAsync();
        return ResponseEntity.accepted().body("Batch analysis started");
    }

    @PostMapping("/batch/force")
    public ResponseEntity<String> forceBatchAnalyze() {
        analysisService.forceReanalyzeAllAsync();
        return ResponseEntity.accepted().body("Force batch analysis started");
    }

    @PostMapping("/{newsItemId}/manual")
    public ResponseEntity<NewsAnalysis> manualAnalyze(
            @PathVariable String newsItemId,
            @RequestBody AnalysisOptions options,
            Authentication auth) {
        String username = null;
        if (auth != null) {
            username = userRepository.findByEmail(auth.getName())
                    .map(u -> u.getUsername())
                    .orElse(auth.getName());
        }
        try {
            return ResponseEntity.ok(analysisService.analyzeManual(newsItemId, username, options));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{newsItemId}/manual")
    public ResponseEntity<Void> deleteManualAnalysis(
            @PathVariable String newsItemId,
            Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        String username = userRepository.findByEmail(auth.getName())
                .map(u -> u.getUsername()).orElse(null);
        if (username == null) return ResponseEntity.status(401).build();

        Optional<NewsAnalysis> found = analysisRepository.findByNewsItemId(newsItemId);
        if (found.isEmpty()) return ResponseEntity.notFound().build();

        NewsAnalysis analysis = found.get();
        if (!username.equals(analysis.getAnalyzedByUsername())) {
            return ResponseEntity.status(403).build();
        }

        analysisRepository.deleteById(analysis.getId());
        return ResponseEntity.noContent().build();
    }
}
