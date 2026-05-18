package com.lake_team.fistserios.controller.rest;

import com.lake_team.fistserios.model.User;
import com.lake_team.fistserios.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/user/favorites")
@RequiredArgsConstructor
public class UserFavoritesController {

    private final UserRepository userRepository;

    /** Переключити улюблену статтю (додати / видалити) */
    @PostMapping("/{newsItemId}")
    public ResponseEntity<Map<String, Object>> toggle(
            @PathVariable String newsItemId,
            Authentication auth) {

        if (auth == null) return ResponseEntity.status(401).build();

        User user = userRepository.findByEmail(auth.getName())
                .orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        Set<String> favs = user.getFavoriteNewsIds();
        boolean added;
        if (favs.contains(newsItemId)) {
            favs.remove(newsItemId);
            added = false;
        } else {
            favs.add(newsItemId);
            added = true;
        }
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("added", added, "totalFavorites", favs.size()));
    }

    /** Явне видалення з улюблених (без toggle) */
    @DeleteMapping("/{newsItemId}")
    public ResponseEntity<Void> remove(
            @PathVariable String newsItemId,
            Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        user.getFavoriteNewsIds().remove(newsItemId);
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    /** Отримати всі id улюблених статей поточного користувача */
    @GetMapping
    public ResponseEntity<Set<String>> getAll(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        return userRepository.findByEmail(auth.getName())
                .map(u -> ResponseEntity.ok(u.getFavoriteNewsIds()))
                .orElse(ResponseEntity.status(401).build());
    }
}
