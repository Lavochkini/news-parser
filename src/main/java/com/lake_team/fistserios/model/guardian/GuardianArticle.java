package com.lake_team.fistserios.model.guardian;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GuardianArticle {
    private String id;
    private String sectionId;
    private String sectionName;
    private String webPublicationDate;
    private String webTitle;
    private String webUrl;
    private GuardianArticleFields fields;
}
