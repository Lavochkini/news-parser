package com.lake_team.fistserios.analysis;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FactCheckClaim {
    private final String claimText;   // текст заяви
    private final String rating;      // "True", "False", "Misleading" тощо
    private final String publisher;   // PolitiFact, Snopes, Reuters...
    private final String url;         // посилання на повну перевірку
}
