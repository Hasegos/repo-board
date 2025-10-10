package io.github.repoboard.repository;

import io.github.repoboard.model.DeleteUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface DeleteUserRepository extends JpaRepository<DeleteUser, Long> {
    Optional<DeleteUser> findByUsername(String username);
    void deleteAllByDeleteAtBefore(Instant threshold);
    Optional<DeleteUser> findByProviderId(String providerId);
}