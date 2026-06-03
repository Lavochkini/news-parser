package com.lake_team.fistserios.controller.rest;

import com.lake_team.fistserios.security.JwtUtil;
import com.lake_team.fistserios.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        boolean taken = userService.ifUserExistByUsername(username.trim());
        return ResponseEntity.ok(Map.of("taken", taken));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email    = body.getOrDefault("login", body.get("email"));
        String password = body.get("password");

        return userService.login(email, password)
                .map(user -> {
                    String token = jwtUtil.generateToken(user);
                    return ResponseEntity.ok(Map.of(
                            "token",    token,
                            "userId",   user.getId(),
                            "username", user.getUsername(),
                            "email",    user.getEmail(),
                            "role",     user.getRole().name()
                    ));
                })
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid email or password")));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email    = body.get("email");
        String password = body.get("password");

        List<Map<String, String>> errors = new ArrayList<>();

        if (userService.ifUserExistByEmail(email)) {
            errors.add(Map.of("field", "email", "message", "Email is already taken"));
        }
        if (userService.ifUserExistByUsername(username)) {
            errors.add(Map.of("field", "username", "message", "Username is already taken"));
        }
        if (!errors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("errors", errors));
        }

        try {
            var savedUser = userService.registerUser(username, email, password);
            String token  = jwtUtil.generateToken(savedUser);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "token",    token,
                    "userId",   savedUser.getId(),
                    "username", savedUser.getUsername(),
                    "email",    savedUser.getEmail(),
                    "role",     savedUser.getRole().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }
}
