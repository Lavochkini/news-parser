package com.lake_team.fistserios.controller;/*
  @author Bogdan
  @project fistserios
  @class UserController
  @version 1.0.0
  @since 28.08.2025 - 21.06
*/

import com.lake_team.fistserios.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.lake_team.fistserios.service.UserService;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService){
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        Optional<User> optionalUser = userService.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            if (encoder.matches(password, user.getPassword())) { // порівнюємо хеш з введеним паролем
                Map<String, String> response = Map.of(
                        "status", "success",
                        "username", user.getUsername(),
                        "email", user.getEmail()
                );
                return ResponseEntity.ok(response);
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "error"));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");

        try {
            User savedUser = userService.registerUser(username, email, password);

            // Повертаємо JSON
            Map<String, Object> response = Map.of(
                    "status", "success",
                    "message", "User registered successfully",
                    "userId", savedUser.getId()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                    "status", "error",
                    "message", "Registration failed: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}