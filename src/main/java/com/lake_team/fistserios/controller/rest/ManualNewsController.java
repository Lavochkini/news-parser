package com.lake_team.fistserios.controller.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.model.NewsSourceType;
import com.lake_team.fistserios.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/news/manual")
@RequiredArgsConstructor
@CrossOrigin
public class ManualNewsController {

    private final NewsRepository newsRepository;
    private final ObjectMapper   objectMapper;

    /* ── Одна стаття з форми ── */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addOne(@RequestBody ManualArticleRequest req) {
        if (req.title() == null || req.title().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Заголовок не може бути порожнім"));
        }

        // Перевірка дублікату за заголовком
        if (newsRepository.findByTitle(req.title().trim()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of(
                "error",     "Стаття з таким заголовком вже існує в базі",
                "duplicate", true
            ));
        }

        NewsItem item = buildItem(req);
        try {
            NewsItem saved = newsRepository.save(item);
            return ResponseEntity.ok(Map.of(
                "id",      saved.getId(),
                "title",   saved.getTitle(),
                "message", "Статтю додано"
            ));
        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(409).body(Map.of(
                "error",     "Стаття з таким URL вже існує в базі",
                "duplicate", true
            ));
        }
    }

    /* ── Масовий імпорт з JSON-файлу ── */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Файл порожній"));
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (!filename.endsWith(".json")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Підтримується тільки .json формат"));
        }

        List<ManualArticleRequest> requests;
        try {
            requests = objectMapper.readValue(
                file.getInputStream(), new TypeReference<>() {});
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Не вдалося розпарсити JSON: " + e.getMessage()));
        }

        int imported = 0, skipped = 0;
        List<String> ids = new ArrayList<>();
        for (ManualArticleRequest req : requests) {
            // Пропускаємо дублікат за заголовком
            if (req.title() != null && !req.title().isBlank() &&
                newsRepository.findByTitle(req.title().trim()).isPresent()) {
                skipped++;
                continue;
            }
            try {
                NewsItem saved = newsRepository.save(buildItem(req));
                ids.add(saved.getId());
                imported++;
            } catch (DuplicateKeyException e) {
                // Дублікат за URL
                skipped++;
            }
        }

        return ResponseEntity.ok(Map.of(
            "imported", imported,
            "skipped",  skipped,
            "ids",      ids,
            "message",  "Імпортовано " + imported + " статей, пропущено дублікатів: " + skipped
        ));
    }

    private NewsItem buildItem(ManualArticleRequest r) {
        String url = (r.url() != null && !r.url().isBlank())
                ? r.url()
                : "manual://" + UUID.randomUUID();

        return NewsItem.builder()
                .title(r.title())
                .description(r.description())
                .fullContent(r.fullContent() != null ? r.fullContent() : r.description())
                .url(url)
                .imageUrl(r.imageUrl())
                .source(r.source() != null ? r.source() : "Manual")
                .category(r.category())
                .publishedAt(r.publishedAt() != null ? r.publishedAt() : LocalDateTime.now())
                .sourceType(NewsSourceType.MANUAL)
                .build();
    }

    public record ManualArticleRequest(
        String        title,
        String        description,
        String        fullContent,
        String        url,
        String        imageUrl,
        String        source,
        String        category,
        LocalDateTime publishedAt
    ) {}
}
