package com.lake_team.fistserios.model.guardian;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GuardianArticleFields {
    private String thumbnail;
    private String trailText;
    private String bodyText;
}
