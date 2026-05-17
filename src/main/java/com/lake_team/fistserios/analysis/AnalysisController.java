package com.lake_team.fistserios.analysis;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
@CrossOrigin
public class AnalysisController {

    private final NewsAnalysisService analysisService;

    /** Отримати результат аналізу для статті (або запустити якщо ще немає) */
    @GetMapping("/{newsItemId}")
    public ResponseEntity<NewsAnalysis> getOrAnalyze(@PathVariable String newsItemId) {
        try {
            return ResponseEntity.ok(analysisService.analyzeArticle(newsItemId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Повторний аналіз статті */
    @PostMapping("/{newsItemId}/reanalyze")
    public ResponseEntity<NewsAnalysis> reanalyze(@PathVariable String newsItemId) {
        try {
            return ResponseEntity.ok(analysisService.reanalyzeArticle(newsItemId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Запустити аналіз всіх статей без результату (async) */
    @PostMapping("/batch")
    public ResponseEntity<String> batchAnalyze() {
        analysisService.analyzeAllAsync();
        return ResponseEntity.accepted().body("Batch analysis started");
    }

    /** Видалити всі аналізи і запустити заново (async) */
    @PostMapping("/batch/force")
    public ResponseEntity<String> forceBatchAnalyze() {
        analysisService.forceReanalyzeAllAsync();
        return ResponseEntity.accepted().body("Force batch analysis started");
    }
}
