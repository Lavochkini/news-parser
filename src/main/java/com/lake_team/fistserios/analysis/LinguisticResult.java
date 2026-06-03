package com.lake_team.fistserios.analysis;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LinguisticResult {

    // ── Core score ──
    private final int score;                        // 0–35

    // ── Існуючі суб-методи ──
    private final List<String> hedgeWordsFound;
    private final List<String> clickbaitIndicators;
    private final List<String> emotionalWordsFound;
    private final List<String> manipulationIndicators;
    private final List<String> conspiracyIndicators;

    // ── Нові суб-методи ──

    // Анонімна атрибуція ("sources say", "insiders claim" без імені)
    private final List<String> anonymousSourcesFound;

    // Щільність цитат — кількість чітких атрибуцій у тексті
    private final int citationCount;               // ≥3 → добре, 0 → погано

    // Узгодженість заголовку та тіла (0.0–1.0 перетин ключових слів)
    private final double headlineBodyOverlap;

    // Закон Беттерідж: заголовок у формі питання
    private final boolean betteridgeDetected;
}
