package com.lake_team.fistserios.model.gnews;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GNewsArticle {
    private String title;
    private String description;
    private String content;
    private String url;
    private String image;
    private String publishedAt;
    private GNewsSource source;
}
