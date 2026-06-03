package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import org.springframework.stereotype.Component;

@Component
public class ReadabilityAnalyzer {

    public record ReadabilityResult(
            double fleschScore,
            String level,
            int wordCount,
            int sentenceCount,
            double avgWordsPerSentence,
            double avgSyllablesPerWord
    ) {}

    public ReadabilityResult analyze(NewsItem item) {
        String text = merge(item.getTitle(), item.getDescription(), item.getFullContent());
        if (text.isBlank()) return empty();

        String[] sentences = text.split("[.!?]+");
        String[] words     = text.replaceAll("[^a-zA-Z\\s]", "").split("\\s+");

        int sentenceCount = Math.max(countNonEmpty(sentences), 1);
        int wordCount     = countNonEmpty(words);
        if (wordCount == 0) return empty();

        int syllableCount = 0;
        for (String word : words) {
            if (!word.isBlank()) syllableCount += countSyllables(word);
        }

        double wordsPerSentence   = (double) wordCount / sentenceCount;
        double syllablesPerWord   = (double) syllableCount / wordCount;
        double flesch = 206.835 - 1.015 * wordsPerSentence - 84.6 * syllablesPerWord;
        flesch = Math.max(0, Math.min(100, flesch));

        return new ReadabilityResult(
                Math.round(flesch * 10.0) / 10.0,
                toLevel(flesch),
                wordCount,
                sentenceCount,
                Math.round(wordsPerSentence * 10.0) / 10.0,
                Math.round(syllablesPerWord * 100.0) / 100.0
        );
    }

    int countSyllables(String word) {
        String w       = word.toLowerCase().replaceAll("[^a-z]", "");
        if (w.isEmpty()) return 0;
        int    count   = 0;
        boolean prevVowel = false;
        for (char c : w.toCharArray()) {
            boolean isVowel = "aeiouy".indexOf(c) >= 0;
            if (isVowel && !prevVowel) count++;
            prevVowel = isVowel;
        }
        if (w.endsWith("e") && count > 1) count--;
        return Math.max(1, count);
    }

    private String toLevel(double score) {
        if (score >= 85) return "VERY_EASY";
        if (score >= 65) return "EASY";
        if (score >= 45) return "STANDARD";
        if (score >= 25) return "DIFFICULT";
        return "VERY_DIFFICULT";
    }

    private int countNonEmpty(String[] arr) {
        int c = 0;
        for (String s : arr) if (s != null && !s.isBlank()) c++;
        return c;
    }

    private String merge(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) if (p != null) sb.append(p).append(' ');
        return sb.toString();
    }

    private ReadabilityResult empty() {
        return new ReadabilityResult(0, "UNKNOWN", 0, 0, 0, 0);
    }
}