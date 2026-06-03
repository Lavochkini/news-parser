package com.lake_team.fistserios.service;

import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.model.NewsSourceType;
import com.lake_team.fistserios.repository.NewsRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Імпортує CSV-файли Kaggle "Fake and Real News Dataset".
 *
 * Очікуваний формат колонок (без заголовка або з заголовком):
 *   title, text, subject, date
 *
 * Використання:
 *   POST /api/dataset/import?truePath=C:/data/True.csv&fakePath=C:/data/Fake.csv
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatasetImportService {

    private final NewsRepository newsRepository;

    public ImportResult importDataset(String trueCsvPath, String fakeCsvPath, int limitPerFile) {
        int imported = 0;
        int skipped  = 0;

        List<String[]> trueRows = readCsv(trueCsvPath);
        List<String[]> fakeRows = readCsv(fakeCsvPath);

        log.info("CSV rows read: TRUE={}, FAKE={}", trueRows.size(), fakeRows.size());

        for (String[] row : trueRows.stream().limit(limitPerFile).toList()) {
            if (saveRow(row, "TRUE")) imported++; else skipped++;
        }
        for (String[] row : fakeRows.stream().limit(limitPerFile).toList()) {
            if (saveRow(row, "FAKE")) imported++; else skipped++;
        }

        log.info("Dataset import complete: {} imported, {} skipped (duplicates)", imported, skipped);
        return new ImportResult(imported, skipped);
    }

    public ImportResult importSingleLabel(String csvPath, String label, int limit) {
        List<String[]> rows = readCsv(csvPath);
        log.info("CSV rows read for label={}: {}", label, rows.size());

        int imported = 0, skipped = 0;
        for (String[] row : rows.stream().limit(limit).toList()) {
            if (saveRow(row, label)) imported++; else skipped++;
        }

        log.info("Import [{}] complete: {} imported, {} skipped", label, imported, skipped);
        return new ImportResult(imported, skipped);
    }

    private boolean saveRow(String[] row, String label) {
        if (row.length < 2) return false;

        String title = clean(row[0]);
        String text  = clean(row[1]);
        if (title.isBlank() || title.equalsIgnoreCase("title")) return false; // пропускаємо заголовок CSV

        String subject = row.length > 2 ? clean(row[2]) : "";
        String date    = row.length > 3 ? clean(row[3]) : "";

        String url = "dataset://kaggle/" + label.toLowerCase() + "/" + UUID.randomUUID();

        NewsItem item = NewsItem.builder()
                .title(title)
                .description(text.length() > 500 ? text.substring(0, 500) : text)
                .fullContent(text)
                .url(url)
                .source("Kaggle Dataset")
                .category(subject.isBlank() ? null : subject)
                .publishedAt(parseDate(date))
                .sourceType(NewsSourceType.DATASET)
                .datasetLabel(label)
                .build();

        try {
            newsRepository.save(item);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    private List<String[]> readCsv(String path) {
        try (CSVReader reader = new CSVReader(new FileReader(path))) {
            return reader.readAll();
        } catch (IOException | CsvException e) {
            log.error("Failed to read CSV {}: {}", path, e.getMessage());
            return List.of();
        }
    }

    private LocalDateTime parseDate(String date) {
        try {
            // Kaggle dataset dates are like "December 31, 2017" or "2017-12-31"
            if (date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDateTime.parse(date + "T00:00:00");
            }
        } catch (Exception ignored) {}
        return LocalDateTime.of(2017, 1, 1, 0, 0);
    }

    private String clean(String s) {
        return s == null ? "" : s.strip();
    }

    public record ImportResult(int imported, int skipped) {}
}
