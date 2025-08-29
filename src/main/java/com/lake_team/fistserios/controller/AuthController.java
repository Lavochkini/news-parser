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
import org.springframework.web.bind.annotation.*;
import com.lake_team.fistserios.service.UserService;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService){
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        boolean success = userService.login(email, password);
        if (success) {
            return ResponseEntity.ok("Login successful!");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        try {
            User savedUser = userService.registerUser(
                    user.getUsername(),
                    user.getEmail(),
                    user.getPassword()
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("User registered successfully with id: " + savedUser.getId());
        }catch (Exception e) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Registration failed: " + e.getMessage());
        }
    }
}