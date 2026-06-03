package com.lake_team.fistserios.security;

import com.lake_team.fistserios.model.Role;
import com.lake_team.fistserios.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User user;

    // 64-byte Base64 secret (512 bits) — required for HS512 / enough for HS256
    private static final String SECRET =
            "dGVzdFNlY3JldEtleVRoYXRJc0xvbmdFbm91Z2hGb3JIVE1BQ1NIQTI1Ngo=";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3_600_000L); // 1 hour

        user = new User("testuser", "test@example.com", "hashedpw", Role.USER);
        ReflectionTestUtils.setField(user, "id", "user-123");
    }

    @Test
    void generateToken_shouldReturnNonBlankToken() {
        String token = jwtUtil.generateToken(user);
        assertThat(token).isNotBlank();
    }

    @Test
    void extractEmail_shouldMatchUserEmail() {
        String token = jwtUtil.generateToken(user);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("test@example.com");
    }

    @Test
    void extractRole_shouldMatchUserRole() {
        String token = jwtUtil.generateToken(user);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("USER");
    }

    @Test
    void extractUserId_shouldMatchUserId() {
        String token = jwtUtil.generateToken(user);
        assertThat(jwtUtil.extractUserId(token)).isEqualTo("user-123");
    }

    @Test
    void isValid_shouldReturnTrueForFreshToken() {
        String token = jwtUtil.generateToken(user);
        assertThat(jwtUtil.isValid(token)).isTrue();
    }

    @Test
    void isValid_shouldReturnFalseForGarbageToken() {
        assertThat(jwtUtil.isValid("not.a.jwt")).isFalse();
    }

    @Test
    void isValid_shouldReturnFalseForExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L); // already expired
        String token = jwtUtil.generateToken(user);
        assertThat(jwtUtil.isValid(token)).isFalse();
    }

    @Test
    void isValid_shouldReturnFalseForTamperedToken() {
        String token = jwtUtil.generateToken(user);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtUtil.isValid(tampered)).isFalse();
    }
}
