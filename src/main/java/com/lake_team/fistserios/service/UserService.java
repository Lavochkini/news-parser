package com.lake_team.fistserios.service;

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
        return userRepository.save(new User(username, email, hashedPassword, Role.USER));
    }

    public Optional<User> login(String emailOrUsername, String rawPassword) {
        Optional<User> user = emailOrUsername.contains("@")
                ? userRepository.findByEmail(emailOrUsername)
                : userRepository.findByUsername(emailOrUsername);
        if (user.isEmpty()) {
            user = userRepository.findByEmail(emailOrUsername);
            if (user.isEmpty()) user = userRepository.findByUsername(emailOrUsername);
        }
        return user.filter(u -> passwordEncoder.matches(rawPassword, u.getPassword()));
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    public Optional<User> updateUser(String id, User updatedUser) {
        return userRepository.findById(id).map(user -> {
            user.setUsername(updatedUser.getUsername());
            user.setEmail(updatedUser.getEmail());
            user.setPassword(updatedUser.getPassword());
            return userRepository.save(user);
        });
    }

    public boolean deleteUser(String id) {
        if (!userRepository.existsById(id)) return false;
        userRepository.deleteById(id);
        return true;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public boolean ifUserExistByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean ifUserExistByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean resetPassword(String userId, String rawPassword) {
        return userRepository.findById(userId).map(user -> {
            user.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
            return true;
        }).orElse(false);
    }
}