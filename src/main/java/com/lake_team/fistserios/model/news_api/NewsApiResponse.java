package com.lake_team.fistserios.model.news_api;/*
  @author Bogdan
  @project fistserios
  @class NewsApiResponce
  @version 1.0.0
  @since 16.09.2025 - 22.24
*/



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsApiResponse {
    private String status;
    private int totalResults;
    private List<NewsApiArticle> articles;
}
