package io.github.repoboard.repository;

import io.github.repoboard.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUserId(Long userId);
    boolean existsByNickname(String nickname);
    boolean existsByUserId(Long userId);
    boolean existsByNicknameAndUserIdNot(String nickname, Long userId);
}