package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Витягує іменовані сутності (власні назви) з тексту та перевіряє їх у Wikipedia REST API.
 *
 * <p>Базова NER-евристика: слова або пари слів, що починаються з великої літери
 * і не є початком речення (не першим словом після крапки/знаку питання/знаку оклику).
 *
 * <p>Це <b>додаткова метрика</b> — не впливає на score, лише зберігається для відображення.
 */
@Component
@Slf4j
public class WikipediaEntityChecker {

    private static final String WIKI_API = "https://en.wikipedia.org/api/rest_v1/page/summary/";
    private static final int    MAX_ENTITIES  = 8;   // щоб не спамити API
    private static final int    REQUEST_DELAY = 200; // ms між запитами

    // Пара або потрійка слів з великої літери (проста NER)
    private static final Pattern CAPITALIZED_PHRASE = Pattern.compile(
            "(?<![.!?]\\s)\\b([A-Z][a-z]{2,}(?:\\s+[A-Z][a-z]{2,}){0,2})\\b"
    );

    // Стоп-слова, що не є сутностями
    private static final Set<String> STOP_ENTITIES = Set.of(
            "The", "This", "That", "These", "Those", "There", "Their",
            "When", "Where", "What", "Which", "Who", "How", "Why",
            "According", "Despite", "During", "After", "Before",
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    );

    private final RestTemplate restTemplate = new RestTemplate();

    public record WikiResult(List<String> checked, List<String> found, int total) {}

    public WikiResult check(NewsItem item) {
        String text = merge(item.getTitle(), item.getDescription(), item.getFullContent());
        List<String> candidates = extractEntities(text);

        List<String> found = new ArrayList<>();

        for (String entity : candidates) {
            try {
                Thread.sleep(REQUEST_DELAY);
                String encoded = URLEncoder.encode(entity.replace(' ', '_'), StandardCharsets.UTF_8);
                String url = WIKI_API + encoded;
                var response = restTemplate.getForObject(url, java.util.Map.class);
                if (response != null && response.containsKey("title")) {
                    found.add(entity);
                    log.debug("Wikipedia confirmed entity: {}", entity);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // 404 або помилка мережі — просто не додаємо в found
                log.trace("Wikipedia entity not found: {} ({})", entity, e.getMessage());
            }
        }

        return new WikiResult(candidates, found, candidates.size());
    }

    /** Витягує унікальні іменовані сутності з тексту (max MAX_ENTITIES). */
    private List<String> extractEntities(String text) {
        if (text == null || text.isBlank()) return List.of();

        Set<String> seen = new LinkedHashSet<>();
        Matcher m = CAPITALIZED_PHRASE.matcher(text);

        while (m.find() && seen.size() < MAX_ENTITIES) {
            String phrase = m.group(1).trim();

            // Пропускаємо стоп-сутності та дуже короткі слова
            if (STOP_ENTITIES.contains(phrase.split("\\s+")[0])) continue;
            if (phrase.length() < 4) continue;

            seen.add(phrase);
        }

        return new ArrayList<>(seen);
    }

    private String merge(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) if (p != null) sb.append(p).append(' ');
        return sb.toString();
    }
}
