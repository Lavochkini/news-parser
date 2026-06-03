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
                // Публічне — перегляд новин, сторінки входу, статичні файли
                .requestMatchers(
                    "/auth/**", "/auth/check-username",
                    "/news/**",
                    "/", "/main", "/login", "/register",
                    "/dashboard", "/my-dashboard", "/add-news", "/admin", "/article/**", "/terms",  // HTML-шаблони — доступ через JS-редирект
                    "/css/**", "/js/**", "/images/**", "/*.json", "/*.html"
                ).permitAll()
                // Захищені API — аналіз, статистика, датасет, додавання новин
                .requestMatchers("/analysis/**").authenticated()
                .requestMatchers("/api/dashboard/**").authenticated()
                .requestMatchers("/api/dataset/**").authenticated()
                .requestMatchers("/api/news/manual", "/api/news/manual/**").authenticated()
                .requestMatchers("/api/user/**").authenticated()
                // Адмін API — тільки ADMIN
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )

            .headers(h -> h.frameOptions(f -> f.disable()))
            .cors(Customizer.withDefaults())

            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
