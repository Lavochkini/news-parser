package com.lake_team.fistserios.service;/*
  @author Bogdan
  @project fistserios
  @class UserService
  @version 1.0.0
  @since 28.08.2025 - 20.34
*/

import com.lake_team.fistserios.model.Role;
import com.lake_team.fistserios.model.User;
import com.lake_team.fistserios.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(String username, String email, String rawPassword) {
        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(username, email, hashedPassword, Role.USER);
        return userRepository.save(user);
    }

    public boolean login(String email, String rawPassword) {
        User user = findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Ноунейм проблема в тобі"));

        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}