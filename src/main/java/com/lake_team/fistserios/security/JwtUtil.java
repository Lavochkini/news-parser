package com.lake_team.fistserios.security;

import com.lake_team.fistserios.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration; // мілісекунди, напр. 86400000 = 24 год

    // Будуємо крипто-ключ із Base64-рядка з конфігурації
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /**
     * Генерує JWT для конкретного юзера.
     * Payload містить: email (subject), userId, username, role, iat, exp.
     */
    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())             // "sub" — ідентифікатор
                .claim("userId",   user.getId())
                .claim("username", user.getUsername())
                .claim("role",     user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())                   // підписуємо HS256
                .compact();
    }

    // Розпарсовує токен і повертає payload (Claims)
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())                 // перевіряє підпис
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public String extractUserId(String token) {
        return extractClaims(token).get("userId", String.class);
    }

    /**
     * Повертає true тільки якщо:
     *   1. Підпис валідний (токен не підроблений)
     *   2. Токен не прострочений (exp > now)
     */
    public boolean isValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
