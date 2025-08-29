package com.lake_team.fistserios.repository;/*
  @author Bogdan
  @project fistserios
  @class UserRepository
  @version 1.0.0
  @since 28.08.2025 - 20.49
*/

import com.lake_team.fistserios.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
