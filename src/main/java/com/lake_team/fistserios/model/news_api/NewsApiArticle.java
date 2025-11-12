package com.lake_team.fistserios.model.news_api;/*
  @author Bogdan
  @project fistserios
  @class NewsApiArticle
  @version 1.0.0
  @since 16.09.2025 - 22.22
*/

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsApiArticle {
    private NewsApiSource source;
    private String author;
    private String title;
    private String description;
    private String url;
    private String urlToImage;
    private String publishedAt;
    private String content;
}