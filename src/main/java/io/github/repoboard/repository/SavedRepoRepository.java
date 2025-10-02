package io.github.repoboard.repository;

import io.github.repoboard.model.SavedRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavedRepoRepository extends JpaRepository<SavedRepo, Long> {
    Optional<SavedRepo> findByRepoGithubIdAndUserId(Long repoGithubId, Long userId);
    boolean existsByRepoGithubIdAndUserId(Long repoGithubId, Long userId);

    Page<SavedRepo> findAllByUserId(Long userId, Pageable pageable);
    Page<SavedRepo> findAllByUserIdAndLanguageMainIgnoreCase(Long userId, String language, Pageable pageable);
    Page<SavedRepo> findAllByUserIdAndIsPinnedTrue(Long userId, Pageable pageable);
    Page<SavedRepo> findAllByUserIdAndIsPinnedTrueAndLanguageMainIgnoreCase(Long userId,String language, Pageable pageable);
    Page<SavedRepo> findAllByUserIdAndIsPinnedFalse(Long userId, Pageable pageable);
    Page<SavedRepo> findAllByUserIdAndIsPinnedFalseAndLanguageMainIgnoreCase(Long userId, String language, Pageable pageable);
}