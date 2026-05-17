package com.lake_team.fistserios.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "news")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "url")
@ToString
public class NewsItem {

    @Id
    private String id;

    private String title;

    private String description;

    private String fullContent;

    @Indexed(unique = true)
    private String url;

    private String imageUrl;

    private LocalDateTime publishedAt;

    private String source;

    @Indexed
    private String category;

    @Indexed
    private NewsSourceType sourceType;

    // Мітка з датасету: "TRUE" або "FAKE" (null для живих новин)
    private String datasetLabel;
}
