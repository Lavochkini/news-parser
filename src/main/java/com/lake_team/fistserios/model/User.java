package com.lake_team.fistserios.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "users")
@EqualsAndHashCode
@ToString
@Getter
@Setter
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    @NotBlank @Size(min = 3, max = 50)
    private String username;

    @Indexed(unique = true)
    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;

    private Role role = Role.USER;

    private Set<String> favoriteNewsIds = new HashSet<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public User() {}

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public User(String username, String email, String password, Role role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }
}
