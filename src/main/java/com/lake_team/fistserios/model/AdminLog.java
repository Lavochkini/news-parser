package com.lake_team.fistserios.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "admin_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminLog {
    @Id
    private String id;
    private String action;        // CHANGE_ROLE, DELETE_USER, EDIT_NEWS, FETCH, ...
    private String details;
    private String adminUsername;
    private LocalDateTime createdAt;
}
