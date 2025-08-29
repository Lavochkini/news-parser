package com.lake_team.fistserios.model;/*
  @author Bogdan
  @project fistserios
  @class User
  @version 1.0.0
  @since 27.08.2025 - 18.52
*/


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@EqualsAndHashCode
@ToString
@Entity
@Table(name = "users")
@Getter
@Setter

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    @NotBlank @Size(min = 3, max = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    @NotBlank
    @Email
    private String email;

    @Column(nullable = false)
    @NotBlank
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public User(){

    }

    public User(String username, String email, String password){
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public User( String username, String email, String password, Role role){
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }
}