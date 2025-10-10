package io.github.repoboard.repository;

import io.github.repoboard.model.Profile;
import io.github.repoboard.model.enums.ProfileVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUserId(Long userId);
    Optional<Profile> findByGithubLoginAndProfileVisibility(String githubLogin, ProfileVisibility profileVisibility);
    boolean existsByUserId(Long userId);
}