package com.lake_team.fistserios.service;

import com.lake_team.fistserios.model.Role;
import com.lake_team.fistserios.model.User;
import com.lake_team.fistserios.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User("alice", "alice@example.com", "$2a$10$hashedpw", Role.USER);
        ReflectionTestUtils.setField(existingUser, "id", "id-1");
    }

    @Test
    void registerUser_shouldSaveUserWithHashedPassword() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        User result = userService.registerUser("alice", "alice@example.com", "rawPass");
        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getPassword()).doesNotContain("rawPass");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void login_withCorrectPassword_shouldReturnUser() {
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("secret");
        User stored = new User("alice", "alice@example.com", hash, Role.USER);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(stored));

        Optional<User> result = userService.login("alice@example.com", "secret");
        assertThat(result).isPresent();
    }

    @Test
    void login_withWrongPassword_shouldReturnEmpty() {
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("secret");
        User stored = new User("alice", "alice@example.com", hash, Role.USER);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(stored));

        Optional<User> result = userService.login("alice@example.com", "wrongpassword");
        assertThat(result).isEmpty();
    }

    @Test
    void login_unknownEmail_shouldReturnEmpty() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
        assertThat(userService.login("nobody@example.com", "any")).isEmpty();
    }

    @Test
    void findByEmail_shouldDelegateToRepository() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existingUser));
        assertThat(userService.findByEmail("alice@example.com")).contains(existingUser);
    }

    @Test
    void findById_shouldDelegateToRepository() {
        when(userRepository.findById("id-1")).thenReturn(Optional.of(existingUser));
        assertThat(userService.findById("id-1")).contains(existingUser);
    }

    @Test
    void updateUser_whenExists_shouldUpdateFieldsAndSave() {
        User patch = new User("alice2", "alice2@example.com", "newpw");
        when(userRepository.findById("id-1")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<User> result = userService.updateUser("id-1", patch);
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("alice2");
        assertThat(result.get().getEmail()).isEqualTo("alice2@example.com");
    }

    @Test
    void updateUser_whenNotFound_shouldReturnEmpty() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());
        assertThat(userService.updateUser("missing", existingUser)).isEmpty();
    }

    @Test
    void deleteUser_whenExists_shouldReturnTrue() {
        when(userRepository.existsById("id-1")).thenReturn(true);
        assertThat(userService.deleteUser("id-1")).isTrue();
        verify(userRepository).deleteById("id-1");
    }

    @Test
    void deleteUser_whenNotExists_shouldReturnFalse() {
        when(userRepository.existsById("missing")).thenReturn(false);
        assertThat(userService.deleteUser("missing")).isFalse();
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void getAllUsers_shouldReturnAllFromRepository() {
        when(userRepository.findAll()).thenReturn(List.of(existingUser));
        assertThat(userService.getAllUsers()).hasSize(1);
    }

    @Test
    void ifUserExistByEmail_shouldDelegateToRepository() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);
        assertThat(userService.ifUserExistByEmail("alice@example.com")).isTrue();
    }

    @Test
    void ifUserExistByUsername_shouldDelegateToRepository() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);
        assertThat(userService.ifUserExistByUsername("alice")).isTrue();
    }
}
