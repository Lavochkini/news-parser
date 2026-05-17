package com.lake_team.fistserios.controller.rest;

import com.lake_team.fistserios.service.DatasetImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dataset")
@RequiredArgsConstructor
public class DatasetImportController {

    private final DatasetImportService datasetImportService;

    /**
     * POST /api/dataset/import
     *   ?truePath=C:/data/True.csv
     *   &fakePath=C:/data/Fake.csv
     *   &limit=200        (optional, default 200 per file)
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importDataset(
            @RequestParam String truePath,
            @RequestParam String fakePath,
            @RequestParam(defaultValue = "200") int limit) {

        DatasetImportService.ImportResult result =
                datasetImportService.importDataset(truePath, fakePath, limit);

        return ResponseEntity.ok(Map.of(
                "imported", result.imported(),
                "skipped",  result.skipped(),
                "message",  "Import complete. Run POST /analysis/batch to analyze."
        ));
    }
}
