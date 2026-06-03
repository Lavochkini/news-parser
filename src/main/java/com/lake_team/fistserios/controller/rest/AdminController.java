package com.lake_team.fistserios.controller.rest;

import com.lake_team.fistserios.analysis.NewsAnalysis;
import com.lake_team.fistserios.analysis.NewsAnalysisRepository;
import com.lake_team.fistserios.analysis.NewsAnalysisService;
import com.lake_team.fistserios.analysis.PartialBatchOptions;
import com.lake_team.fistserios.model.AdminLog;
import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.model.Role;
import com.lake_team.fistserios.model.User;
import com.lake_team.fistserios.repository.AdminLogRepository;
import com.lake_team.fistserios.repository.NewsRepository;
import com.lake_team.fistserios.repository.UserRepository;
import com.lake_team.fistserios.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository         userRepository;
    private final NewsRepository         newsRepository;
    private final NewsAnalysisRepository analysisRepository;
    private final AdminLogRepository     logRepository;
    private final NewsAnalysisService    analysisService;
    private final UserService            userService;
    private final NewsApiService         newsApiService;
    private final GuardianService        guardianService;
    private final GNewsService           gNewsService;
    private final RedditService          redditService;

    /* ══════════════════════════════════════
       HELPERS
    ══════════════════════════════════════ */

    private String adminName(Authentication auth) {
        if (auth == null) return "?";
        return userRepository.findByEmail(auth.getName())
                .map(User::getUsername)
                .orElse(auth.getName());
    }

    private void log(Authentication auth, String action, String details) {
        logRepository.save(AdminLog.builder()
                .action(action)
                .details(details)
                .adminUsername(adminName(auth))
                .createdAt(LocalDateTime.now())
                .build());
    }

    private Map<String, Object> toUserDto(User u) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id",            u.getId());
        dto.put("username",      u.getUsername());
        dto.put("email",         u.getEmail());
        dto.put("role",          u.getRole().name());
        dto.put("createdAt",     u.getCreatedAt());
        dto.put("favoriteCount", u.getFavoriteNewsIds() != null ? u.getFavoriteNewsIds().size() : 0);
        return dto;
    }

    /* ══════════════════════════════════════
       OVERVIEW STATS
    ══════════════════════════════════════ */

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long totalUsers    = userRepository.count();
        long totalNews     = newsRepository.count();
        long totalAnalyses = analysisRepository.count();
        long adminCount    = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN).count();

        double avgScore = analysisRepository.findAll().stream()
                .mapToInt(NewsAnalysis::getCredibilityScore)
                .average().orElse(0);

        Map<String, Long> bySource = newsRepository.findAll().stream()
                .filter(n -> n.getSourceType() != null)
                .collect(Collectors.groupingBy(n -> n.getSourceType().name(), Collectors.counting()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers",    totalUsers);
        result.put("totalNews",     totalNews);
        result.put("totalAnalyses", totalAnalyses);
        result.put("adminCount",    adminCount);
        result.put("avgScore",      Math.round(avgScore * 10.0) / 10.0);
        result.put("bySource",      bySource);
        return ResponseEntity.ok(result);
    }

    /* ══════════════════════════════════════
       USERS — LIST
    ══════════════════════════════════════ */

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "15") int size) {

        List<User> all = userRepository.findAll();

        if (!search.isBlank()) {
            String q = search.toLowerCase();
            all = all.stream()
                    .filter(u -> u.getUsername().toLowerCase().contains(q)
                              || u.getEmail().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }

        all.sort(Comparator
                .comparingInt((User u) -> u.getRole() == Role.ADMIN ? 0 : 1)
                .thenComparing(u -> u.getCreatedAt() == null ? "" : u.getCreatedAt().toString(),
                        Comparator.reverseOrder()));

        int total = all.size();
        int from  = page * size;
        int to    = Math.min(from + size, total);
        List<User> paged = from < total ? all.subList(from, to) : List.of();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content",       paged.stream().map(this::toUserDto).collect(Collectors.toList()));
        result.put("totalElements", total);
        result.put("totalPages",    (int) Math.ceil((double) total / size));
        result.put("page",          page);
        return ResponseEntity.ok(result);
    }

    /* ══════════════════════════════════════
       USERS — DETAILS
    ══════════════════════════════════════ */

    @GetMapping("/users/{id}/details")
    public ResponseEntity<?> getUserDetails(@PathVariable String id) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        User user = opt.get();
        List<NewsAnalysis> analyses = analysisRepository
                .findAllByAnalyzedByUsernameOrderByAnalyzedAtDesc(user.getUsername());

        List<Map<String, Object>> recentDtos = analyses.stream().limit(10).map(a -> {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("id",               a.getId());
            d.put("newsItemId",       a.getNewsItemId());
            d.put("credibilityScore", a.getCredibilityScore());
            d.put("analyzedAt",       a.getAnalyzedAt());
            newsRepository.findById(a.getNewsItemId())
                    .ifPresent(n -> d.put("newsTitle", n.getTitle()));
            return d;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user",           toUserDto(user));
        result.put("totalAnalyses",  analyses.size());
        result.put("recentAnalyses", recentDtos);
        return ResponseEntity.ok(result);
    }

    /* ══════════════════════════════════════
       USERS — CHANGE ROLE
    ══════════════════════════════════════ */

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> changeRole(
            @PathVariable String id,
            @RequestBody  Map<String, String> body,
            Authentication auth) {

        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        User user = opt.get();
        try {
            Role newRole = Role.valueOf(body.get("role"));
            user.setRole(newRole);
            userRepository.save(user);
            log(auth, "CHANGE_ROLE", user.getUsername() + " → " + newRole.name());
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("ok",   true);
            r.put("role", user.getRole().name());
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("error", "Невідома роль: " + body.get("role"));
            return ResponseEntity.badRequest().body(r);
        }
    }

    /* ══════════════════════════════════════
       USERS — EDIT
    ══════════════════════════════════════ */

    @PutMapping("/users/{id}")
    public ResponseEntity<?> editUser(
            @PathVariable String id,
            @RequestBody  Map<String, String> body,
            Authentication auth) {

        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        User user = opt.get();
        String newUsername = body.get("username");
        String newEmail    = body.get("email");
        String newPassword = body.get("password");

        if (newUsername != null && !newUsername.isBlank()) {
            if (!newUsername.trim().equals(user.getUsername())
                    && userRepository.existsByUsername(newUsername.trim())) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("error", "Username вже зайнятий");
                return ResponseEntity.badRequest().body(err);
            }
            user.setUsername(newUsername.trim());
        }
        if (newEmail != null && !newEmail.isBlank()) {
            if (!newEmail.trim().equals(user.getEmail())
                    && userRepository.existsByEmail(newEmail.trim())) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("error", "Email вже зайнятий");
                return ResponseEntity.badRequest().body(err);
            }
            user.setEmail(newEmail.trim());
        }
        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 6) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("error", "Пароль має бути не менше 6 символів");
                return ResponseEntity.badRequest().body(err);
            }
            userService.resetPassword(id, newPassword);
        }
        userRepository.save(user);
        log(auth, "EDIT_USER", "Редагування: " + user.getUsername());
        return ResponseEntity.ok(toUserDto(user));
    }

    /* ══════════════════════════════════════
       USERS — DELETE
    ══════════════════════════════════════ */

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id, Authentication auth) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        String username = opt.get().getUsername();
        userRepository.deleteById(id);
        log(auth, "DELETE_USER", "Видалено: " + username);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        return ResponseEntity.ok(r);
    }

    /* ══════════════════════════════════════
       USERS — BULK DELETE
    ══════════════════════════════════════ */

    @PostMapping("/users/bulk-delete")
    public ResponseEntity<?> bulkDeleteUsers(
            @RequestBody Map<String, List<String>> body,
            Authentication auth) {

        List<String> ids = body.getOrDefault("ids", List.of());
        ids.forEach(userRepository::deleteById);
        log(auth, "BULK_DELETE_USERS", "Видалено: " + ids.size() + " користувачів");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("deleted", ids.size());
        return ResponseEntity.ok(r);
    }

    /* ══════════════════════════════════════
       NEWS — LIST
    ══════════════════════════════════════ */

    @GetMapping("/news")
    public ResponseEntity<?> getNews(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String source,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "15") int size) {

        List<NewsItem> all = newsRepository.findAll();

        if (!search.isBlank()) {
            String q = search.toLowerCase();
            all = all.stream()
                    .filter(n -> (n.getTitle()  != null && n.getTitle().toLowerCase().contains(q))
                              || (n.getSource() != null && n.getSource().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }
        if (!source.isBlank()) {
            all = all.stream()
                    .filter(n -> n.getSourceType() != null && n.getSourceType().name().equals(source))
                    .collect(Collectors.toList());
        }

        all.sort(Comparator.comparing(
                (NewsItem n) -> n.getPublishedAt() == null ? "" : n.getPublishedAt().toString(),
                Comparator.reverseOrder()));

        int total = all.size();
        int from  = page * size;
        int to    = Math.min(from + size, total);
        List<NewsItem> paged = from < total ? all.subList(from, to) : List.of();

        List<Map<String, Object>> dtos = paged.stream().map(n -> {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("id",          n.getId());
            d.put("title",       n.getTitle());
            d.put("source",      n.getSource());
            d.put("category",    n.getCategory());
            d.put("sourceType",  n.getSourceType() != null ? n.getSourceType().name() : null);
            d.put("publishedAt", n.getPublishedAt());
            d.put("url",         n.getUrl());
            d.put("analyzed",    analysisRepository.existsByNewsItemId(n.getId()));
            return d;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content",       dtos);
        result.put("totalElements", total);
        result.put("totalPages",    (int) Math.ceil((double) total / size));
        result.put("page",          page);
        return ResponseEntity.ok(result);
    }

    /* ══════════════════════════════════════
       NEWS — EDIT
    ══════════════════════════════════════ */

    @PutMapping("/news/{id}")
    public ResponseEntity<?> editNews(
            @PathVariable String id,
            @RequestBody  Map<String, String> body,
            Authentication auth) {

        Optional<NewsItem> opt = newsRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        NewsItem news = opt.get();
        String newTitle    = body.get("title");
        String newSource   = body.get("source");
        String newCategory = body.get("category");

        if (newTitle != null && !newTitle.isBlank()) news.setTitle(newTitle.trim());
        if (newSource   != null) news.setSource(newSource.trim().isEmpty()    ? null : newSource.trim());
        if (newCategory != null) news.setCategory(newCategory.trim().isEmpty() ? null : newCategory.trim());

        newsRepository.save(news);
        log(auth, "EDIT_NEWS", "Редагування: " + news.getTitle());

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok",    true);
        r.put("title", news.getTitle());
        return ResponseEntity.ok(r);
    }

    /* ══════════════════════════════════════
       NEWS — DELETE
    ══════════════════════════════════════ */

    @DeleteMapping("/news/{id}")
    public ResponseEntity<?> deleteNews(@PathVariable String id, Authentication auth) {
        Optional<NewsItem> opt = newsRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        String title = opt.get().getTitle();
        newsRepository.deleteById(id);
        analysisRepository.findByNewsItemId(id)
                .ifPresent(a -> analysisRepository.deleteById(a.getId()));
        log(auth, "DELETE_NEWS", "Видалено: " + title);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        return ResponseEntity.ok(r);
    }

    /* ══════════════════════════════════════
       NEWS — BULK DELETE
    ══════════════════════════════════════ */

    @PostMapping("/news/bulk-delete")
    public ResponseEntity<?> bulkDeleteNews(
            @RequestBody Map<String, List<String>> body,
            Authentication auth) {

        List<String> ids = body.getOrDefault("ids", List.of());
        ids.forEach(newsId -> {
            newsRepository.deleteById(newsId);
            analysisRepository.findByNewsItemId(newsId)
                    .ifPresent(a -> analysisRepository.deleteById(a.getId()));
        });
        log(auth, "BULK_DELETE_NEWS", "Видалено: " + ids.size() + " новин");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("deleted", ids.size());
        return ResponseEntity.ok(r);
    }

    /* ══════════════════════════════════════
       ANALYSES — LIST
    ══════════════════════════════════════ */

    @GetMapping("/analyses")
    public ResponseEntity<?> getAnalyses(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "15") int size) {

        List<NewsAnalysis> all = analysisRepository.findAll();

        if (!search.isBlank()) {
            String q = search.toLowerCase();
            all = all.stream()
                    .filter(a -> a.getAnalyzedByUsername() != null
                              && a.getAnalyzedByUsername().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }

        all.sort(Comparator.comparing(
                (NewsAnalysis a) -> a.getAnalyzedAt() == null ? "" : a.getAnalyzedAt().toString(),
                Comparator.reverseOrder()));

        int total = all.size();
        int from  = page * size;
        int to    = Math.min(from + size, total);
        List<NewsAnalysis> paged = from < total ? all.subList(from, to) : List.of();

        // Batch title lookup to avoid N+1
        Set<String> newsIds = paged.stream()
                .map(NewsAnalysis::getNewsItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> titleMap = newsRepository.findAllById(newsIds).stream()
                .collect(Collectors.toMap(
                        NewsItem::getId,
                        n -> n.getTitle() != null ? n.getTitle() : "—"));

        List<Map<String, Object>> dtos = paged.stream().map(a -> {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("id",                 a.getId());
            d.put("newsItemId",         a.getNewsItemId());
            d.put("newsTitle",          titleMap.getOrDefault(a.getNewsItemId(), "—"));
            d.put("credibilityScore",   a.getCredibilityScore());
            d.put("linguisticScore",    a.getLinguisticScore());
            d.put("crossSourceScore",   a.getCrossSourceScore());
            d.put("factCheckScore",     a.getFactCheckScore());
            d.put("analyzedByUsername", a.getAnalyzedByUsername());
            d.put("isManual",           a.isManual());
            d.put("sentimentLabel",     a.getSentimentLabel());
            d.put("analyzedAt",         a.getAnalyzedAt());
            return d;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content",       dtos);
        result.put("totalElements", total);
        result.put("totalPages",    (int) Math.ceil((double) total / size));
        result.put("page",          page);
        return ResponseEntity.ok(result);
    }

    /* ══════════════════════════════════════
       ANALYSES — DELETE
    ══════════════════════════════════════ */

    @DeleteMapping("/analyses/{id}")
    public ResponseEntity<?> deleteAnalysis(@PathVariable String id, Authentication auth) {
        if (!analysisRepository.existsById(id)) return ResponseEntity.notFound().build();
        analysisRepository.deleteById(id);
        log(auth, "DELETE_ANALYSIS", "ID: " + id);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        return ResponseEntity.ok(r);
    }

    /* ══════════════════════════════════════
       ANALYSES — BULK DELETE
    ══════════════════════════════════════ */

    @PostMapping("/analyses/bulk-delete")
    public ResponseEntity<?> bulkDeleteAnalyses(
            @RequestBody Map<String, List<String>> body,
            Authentication auth) {

        List<String> ids = body.getOrDefault("ids", List.of());
        ids.forEach(analysisRepository::deleteById);
        log(auth, "BULK_DELETE_ANALYSES", "Видалено: " + ids.size() + " аналізів");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("deleted", ids.size());
        return ResponseEntity.ok(r);
    }

    /* ══════════════════════════════════════
       BATCH PARTIAL ANALYSIS
    ══════════════════════════════════════ */

    @PostMapping("/analyses/batch/partial")
    public ResponseEntity<?> partialBatch(
            @RequestBody PartialBatchOptions opts,
            Authentication auth) {

        log(auth, "BATCH_PARTIAL",
                String.format("limit=%s src=%s cat=%s from=%s to=%s force=%s",
                        opts.limit(), opts.sourceType(), opts.category(),
                        opts.dateFrom(), opts.dateTo(), opts.forceReanalyze()));

        NewsAnalysisService.PartialBatchResult result = analysisService.analyzePartialBatch(opts);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total",     result.total());
        r.put("processed", result.processed());
        r.put("skipped",   result.skipped());
        r.put("failed",    result.failed());
        return ResponseEntity.ok(r);
    }

    /* ══════════════════════════════════════
       ANALYTICS
    ══════════════════════════════════════ */

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics() {
        List<User> users = userRepository.findAll();

        List<Map<String, Object>> userStats = users.stream().map(u -> {
            List<NewsAnalysis> analyses = analysisRepository
                    .findAllByAnalyzedByUsernameOrderByAnalyzedAtDesc(u.getUsername());
            double avg = analyses.stream()
                    .mapToInt(NewsAnalysis::getCredibilityScore)
                    .average().orElse(0);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",            u.getId());
            m.put("username",      u.getUsername());
            m.put("email",         u.getEmail());
            m.put("role",          u.getRole().name());
            m.put("analysesCount", analyses.size());
            m.put("avgScore",      Math.round(avg * 10.0) / 10.0);
            m.put("favoriteCount", u.getFavoriteNewsIds() != null ? u.getFavoriteNewsIds().size() : 0);
            m.put("lastActivity",  analyses.isEmpty() ? null : analyses.get(0).getAnalyzedAt());
            m.put("createdAt",     u.getCreatedAt());
            return m;
        }).sorted(Comparator.comparingInt(
                (Map<String, Object> m) -> (int) m.get("analysesCount")).reversed())
          .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("users", userStats);
        return ResponseEntity.ok(result);
    }

    /* ══════════════════════════════════════
       PARSING — TRIGGER FETCH
    ══════════════════════════════════════ */

    @PostMapping("/fetch/{source}")
    public ResponseEntity<?> fetchSource(
            @PathVariable String source,
            Authentication auth) {

        Map<String, Object> r = new LinkedHashMap<>();
        try {
            int saved;
            String src = source.toUpperCase();
            if ("NEWSAPI".equals(src)) {
                saved = newsApiService.fetchAndSaveTopHeadlines("us").size();
            } else if ("GUARDIAN".equals(src)) {
                saved = guardianService.fetchAndSaveNews("world").size();
            } else if ("GNEWS".equals(src)) {
                saved = gNewsService.fetchAndSaveTopHeadlines("en").size();
            } else if ("REDDIT".equals(src)) {
                saved = redditService.fetchAndSaveAll().size();
            } else {
                r.put("error", "Невідоме джерело: " + source);
                return ResponseEntity.badRequest().body(r);
            }
            log(auth, "FETCH", source.toUpperCase() + " → " + saved + " новин");
            r.put("saved",  saved);
            r.put("source", source);
        } catch (Exception e) {
            String msg = e.getMessage();
            r.put("error", msg != null ? msg : "Помилка отримання новин");
        }
        return ResponseEntity.ok(r);
    }

    /* ══════════════════════════════════════
       LOGS
    ══════════════════════════════════════ */

    @GetMapping("/logs")
    public ResponseEntity<?> getLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "25") int size) {

        Page<AdminLog> logPage = logRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content",       logPage.getContent());
        result.put("totalElements", logPage.getTotalElements());
        result.put("totalPages",    logPage.getTotalPages());
        result.put("page",          page);
        return ResponseEntity.ok(result);
    }
}
