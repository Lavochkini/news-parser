package com.lake_team.fistserios.config;/*
  @author Bogdan
  @project fistserios
  @class SecurityConfig
  @version 1.0.0
  @since 29.08.2025 - 19.09
*/

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // вимикаємо CSRF, щоб можна було тестити з Postman
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**",  "/users/**").permitAll()// дозволяємо доступ до наших ендпоїнтів
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated() // все інше вимагає логіну
                )
                .formLogin(form -> form.disable()) // вимикаємо дефолтну форму
                .httpBasic(httpBasic -> httpBasic.disable()); // і базову авторизацію

        return http.build();
    }
}
