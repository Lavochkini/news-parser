package com.lake_team.fistserios.config;

import com.lake_team.fistserios.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig
 *
 * Три зони доступу:
 *
 *   PUBLIC  — будь-хто, токен не потрібен
 *     /auth/**        реєстрація і логін
 *     /news/**        читання новин (публічно)
 *     /main /login /register — Thymeleaf сторінки
 *     /css/** /js/** /images/**
 *
 *   ADMIN   — тільки юзери з роллю ADMIN
 *     /admin/**
 *
 *   AUTHENTICATED — будь-який авторизований юзер
 *     все інше (наприклад /users/**)
 *
 * SessionCreationPolicy.STATELESS — сервер не зберігає сесії.
 * Кожен запит аутентифікується заново через JWT токен.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF вимкнено — не потрібен для stateless REST API з JWT
            .csrf(csrf -> csrf.disable())

            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/auth/**",
                    "/news/**",
                    "/analysis/**",
                    "/api/dashboard/**",
                    "/", "/main", "/login", "/register", "/dashboard",
                    "/css/**", "/js/**", "/images/**"
                ).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )

            .headers(h -> h.frameOptions(f -> f.disable()))
            .cors(Customizer.withDefaults())

            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
