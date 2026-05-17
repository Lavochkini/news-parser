package com.lake_team.fistserios.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Токен відсутній або не Bearer — пропускаємо без аутентифікації
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // відрізаємо "Bearer "

        // Невалідний або прострочений токен
        if (!jwtUtil.isValid(token)) {
            chain.doFilter(request, response);
            return;
        }

        String email = jwtUtil.extractEmail(token);
        String role  = jwtUtil.extractRole(token);

        // Встановлюємо аутентифікацію тільки якщо ще не встановлена
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // "ROLE_USER" або "ROLE_ADMIN" — Spring Security вимагає префікс "ROLE_"
            var authority = new SimpleGrantedAuthority("ROLE_" + role);

            var auth = new UsernamePasswordAuthenticationToken(
                    email,       // principal (хто)
                    null,        // credentials (пароль не потрібен — токен вже перевірений)
                    List.of(authority)
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Кладемо в контекст — тепер Spring знає хто робить запит і яка його роль
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }
}
