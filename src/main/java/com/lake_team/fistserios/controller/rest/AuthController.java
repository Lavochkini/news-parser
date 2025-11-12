package com.lake_team.fistserios.controller.rest;/*
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        return userService.login(email, password)
                .map(user -> ResponseEntity.ok(Map.of(
                        "status", "success",
                        "username", user.getUsername(),
                        "email", user.getEmail()
                )))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("status", "error")));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");

        List<Map<String, String>> errors = new ArrayList<>();

        if (userService.ifUserExistByEmail(email)) {
            errors.add(Map.of(
                    "field", "email",
                    "message", "Registration failed: Email is used"
            ));
        }

        if (userService.ifUserExistByUsername(username)) {
            errors.add(Map.of(
                    "field", "username",
                    "message", "Registration failed: Username is used"
            ));
        }

        if (!errors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of(
                            "status", "error",
                            "errors", errors
                    )
            );
        }

        try {
            User savedUser = userService.registerUser(username, email, password);


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