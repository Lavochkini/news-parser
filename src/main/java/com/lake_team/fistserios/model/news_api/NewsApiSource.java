package com.lake_team.fistserios.model.news_api;/*
  @author Bogdan
  @project fistserios
  @class NewsApiSource
  @version 1.0.0
  @since 16.09.2025 - 22.13
*/

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true) // ігнорує зайві поля
public class NewsApiSource {
    private String id;
    private String name;
}
