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
                "message",  "Import complete."
        ));
    }

    /** Імпортує тільки TRUE або тільки FAKE статті з одного CSV */
    @PostMapping("/import-single")
    public ResponseEntity<Map<String, Object>> importSingle(
            @RequestParam String csvPath,
            @RequestParam(defaultValue = "TRUE") String label,
            @RequestParam(defaultValue = "100") int limit) {

        DatasetImportService.ImportResult result =
                datasetImportService.importSingleLabel(csvPath, label.toUpperCase(), limit);

        return ResponseEntity.ok(Map.of(
                "label",    label.toUpperCase(),
                "imported", result.imported(),
                "skipped",  result.skipped(),
                "message",  "Імпортовано " + result.imported() + " статей [" + label.toUpperCase() + "], пропущено дублікатів: " + result.skipped()
        ));
    }
}
