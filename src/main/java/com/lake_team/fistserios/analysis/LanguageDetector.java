package com.lake_team.fistserios.analysis;

import com.lake_team.fistserios.model.NewsItem;
import org.springframework.stereotype.Component;

/**
 * Простий детектор мови на основі аналізу символів Unicode.
 * Використовує відсоток кириличних літер і мово-специфічні маркери
 * (і/ї/є/ґ для української, ё/ъ для російської).
 */
@Component
public class LanguageDetector {

    public enum Language {
        ENGLISH  ("English",   "en"),
        UKRAINIAN("Ukrainian", "uk"),
        RUSSIAN  ("Russian",   "ru"),
        MIXED    ("Mixed",     "mixed"),
        UNKNOWN  ("Unknown",   "unknown");

        public final String displayName;
        public final String code;

        Language(String displayName, String code) {
            this.displayName = displayName;
            this.code = code;
        }
    }

    public Language detect(NewsItem item) {
        return detect(merge(item.getTitle(), item.getDescription(), item.getFullContent()));
    }

    public Language detect(String text) {
        if (text == null || text.isBlank()) return Language.UNKNOWN;

        long letters  = text.chars().filter(Character::isLetter).count();
        if (letters < 15) return Language.UNKNOWN;

        // Кириличний блок: U+0400 – U+04FF
        long cyrillic = text.chars().filter(c -> c >= 0x0400 && c <= 0x04FF).count();
        double cyrRatio = (double) cyrillic / letters;

        if (cyrRatio > 0.5) {
            // Унікальні для української: і (і/І), ї (ї/Ї), є (є/Є), ґ (ґ/Ґ)
            long ua = text.chars()
                    .filter(c -> c=='і'||c=='І'||c=='ї'||c=='Ї'||c=='є'||c=='Є'||c=='ґ'||c=='Ґ')
                    .count();
            // Унікальні для російської: ё (ё/Ё), ъ (ъ/Ъ)
            long ru = text.chars()
                    .filter(c -> c=='ё'||c=='Ё'||c=='ъ'||c=='Ъ')
                    .count();

            if (ua > 0)       return Language.UKRAINIAN;
            if (ru > ua)      return Language.RUSSIAN;
            return Language.UKRAINIAN; // кирилиця без маркерів → UA за замовчуванням
        }

        if (cyrRatio > 0.15) return Language.MIXED;

        return Language.ENGLISH;
    }

    /** Чи є мова англійською (або невідомою — не обмежуємо). */
    public boolean isEnglishCompatible(Language lang) {
        return lang == Language.ENGLISH || lang == Language.UNKNOWN;
    }

    /**
     * Повертає текст дисклеймера для відображення в UI,
     * або null якщо нічого не потрібно показувати.
     */
    public String buildNote(Language lang) {
        return switch (lang) {
            case UKRAINIAN -> "Мова статті: українська. Лінгвістичний аналіз запущено з розширеними українськими словниками. " +
                              "Вимкнено: Тональність (AFINN-111), Читабельність (Flesch), RSS-перевірка (Snopes/PolitiFact) — " +
                              "ці алгоритми розроблені виключно для англійської мови.";
            case RUSSIAN   -> "Мова статті: російська. Лінгвістичний аналіз обмежений — словники оптимізовані для англійської та " +
                              "української. Вимкнено: Тональність, Читабельність, RSS-перевірка.";
            case MIXED     -> "Виявлено змішану мову (кирилиця + латиниця). Аналіз запущено з обома словниками. " +
                              "Вимкнено: Тональність (AFINN), Читабельність (Flesch), RSS-перевірка.";
            default        -> null; // ENGLISH / UNKNOWN — дисклеймер не потрібен
        };
    }

    private String merge(String... parts) {
        var sb = new StringBuilder();
        for (String p : parts) if (p != null) sb.append(p).append(' ');
        return sb.toString();
    }
}
