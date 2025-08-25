package io.github.repoboard.repository;

import io.github.repoboard.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderId(String providerId);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}