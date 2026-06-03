package com.lake_team.fistserios.analysis;

/**
 * Параметри часткового пакетного аналізу (admin).
 *
 * @param limit           макс кількість статей; null або 0 = без обмеження
 * @param sourceType      фільтр по джерелу (NEWSAPI/GUARDIAN/GNEWS/REDDIT/MANUAL/DATASET); null = всі
 * @param category        фільтр по категорії (регістронезалежно); null = всі
 * @param dateFrom        ISO-дата "yyyy-MM-dd" — включно; null = без нижнього обмеження
 * @param dateTo          ISO-дата "yyyy-MM-dd" — включно; null = без верхнього обмеження
 * @param forceReanalyze  true — перезаписати вже існуючі аналізи
 */
public record PartialBatchOptions(
        Integer limit,
        String  sourceType,
        String  category,
        String  dateFrom,
        String  dateTo,
        boolean forceReanalyze
) {}
