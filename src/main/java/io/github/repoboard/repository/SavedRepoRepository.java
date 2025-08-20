package io.github.repoboard.repository;

import io.github.repoboard.model.SavedRepo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SavedRepoRepository extends JpaRepository<SavedRepo, Long> {
}
