package com.lake_team.fistserios.analysis;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dataset")
@RequiredArgsConstructor
@CrossOrigin
public class DatasetValidationController {

    private final DatasetValidationService validationService;
    private final NewsAnalysisService      analysisService;

    @GetMapping("/validate")
    public Map<String, Object> validate() {
        return validationService.validate();
    }

    @PostMapping("/analyze-batch")
    public Map<String, Object> analyzeBatch(
            @RequestParam(defaultValue = "100") int limit) {
        NewsAnalysisService.BatchResult r = analysisService.analyzeDatasetBatch(limit);
        return Map.of(
                "processed", r.processed(),
                "skipped",   r.skipped(),
                "failed",    r.failed(),
                "message",   "Проаналізовано " + r.processed() + " статей, пропущено " +
                             r.skipped() + " (вже мали аналіз), помилок: " + r.failed()
        );
    }
}
