package com.lake_team.fistserios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println(">>> SecurityConfig is ACTIVE"); // debug marker

        // Disable CSRF for simple JSON API
        http.csrf(csrf -> csrf.disable());

        // Allow everything (temporarily, for testing)
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/**").permitAll()
                .anyRequest().permitAll()
        );

        // Allow H2 console frames (if you use it)
        http.headers(h -> h.frameOptions(f -> f.disable()));

        // Turn off default login forms/basic auth
        http.httpBasic(b -> b.disable());
        http.formLogin(f -> f.disable());

        http.cors(Customizer.withDefaults());
        return http.build();
    }
}
