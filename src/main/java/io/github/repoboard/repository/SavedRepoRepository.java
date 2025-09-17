    package io.github.repoboard.repository;

    import io.github.repoboard.model.SavedRepo;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.stereotype.Repository;

    import java.util.List;
    import java.util.Optional;

    @Repository
    public interface SavedRepoRepository extends JpaRepository<SavedRepo, Long> {
        Optional<SavedRepo> findByRepoGithubIdAndUserId(Long repoGithubId, Long userId);
        List<SavedRepo> findAllByUserId(Long userId);
        void deleteByRepoGithubIdAndUserId(Long repoGithubId, Long userId);
        boolean existsByRepoGithubIdAndUserId(Long repoGithubId, Long userId);
    }