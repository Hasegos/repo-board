package io.github.repoboard.repository;

import io.github.repoboard.model.SavedRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@link SavedRepo} 엔티티에 대한 JPA 리포지토리.
 *
 * <p>사용자가 저장한 GitHub 레포지토리 정보를 조회/관리하는 데 사용된다.</p>
 */
@Repository
public interface SavedRepoRepository extends JpaRepository<SavedRepo, Long> {

    /**
     * GitHub 레포지토리 ID와 사용자 ID로 저장된 레포 조회
     *
     * @param repoGithubId GitHub 레포지토리 ID
     * @param userId 사용자 ID
     * @return 해당 레포(Optional)
     */
    Optional<SavedRepo> findByRepoGithubIdAndUserId(Long repoGithubId, Long userId);

    /**
     * 사용자가 이미 해당 GitHub 레포를 저장했는지 여부 확인
     *
     * @param repoGithubId GitHub 레포지토리 ID
     * @param userId 사용자 ID
     * @return 저장 여부
     */
    boolean existsByRepoGithubIdAndUserId(Long repoGithubId, Long userId);

    /**
     * 사용자가 저장한 모든 레포를 페이지 단위로 조회
     *
     * @param userId 사용자 ID
     * @param pageable 페이지 정보
     * @return 저장된 레포 페이지
     */
    Page<SavedRepo> findAllByUserId(Long userId, Pageable pageable);

    /**
     * 사용자가 저장한 특정 언어 레포를 조회 (대소문자 무시)
     *
     * @param userId 사용자 ID
     * @param language 언어 이름
     * @param pageable 페이지 정보
     * @return 저장된 언어별 레포 페이지
     */
    Page<SavedRepo> findAllByUserIdAndLanguageMainIgnoreCase(Long userId, String language, Pageable pageable);

    /**
     * 사용자가 핀한 레포를 페이지 단위로 조회
     *
     * @param userId 사용자 ID
     * @param pageable 페이지 정보
     * @return 핀된 레포 페이지
     */
    Page<SavedRepo> findAllByUserIdAndIsPinnedTrue(Long userId, Pageable pageable);

    /**
     * 사용자가 핀한 특정 언어 레포 조회 (대소문자 무시)
     *
     * @param userId 사용자 ID
     * @param language 언어 이름
     * @param pageable 페이지 정보
     * @return 핀된 언어별 레포 페이지
     */
    Page<SavedRepo> findAllByUserIdAndIsPinnedTrueAndLanguageMainIgnoreCase(Long userId,String language, Pageable pageable);

    /**
     * 사용자가 핀하지 않은 레포를 페이지 단위로 조회
     *
     * @param userId 사용자 ID
     * @param pageable 페이지 정보
     * @return 미핀 레포 페이지
     */
    Page<SavedRepo> findAllByUserIdAndIsPinnedFalse(Long userId, Pageable pageable);

    /**
     * 사용자가 핀하지 않은 특정 언어 레포 조회 (대소문자 무시)
     *
     * @param userId 사용자 ID
     * @param language 언어 이름
     * @param pageable 페이지 정보
     * @return 미핀 언어별 레포 페이지
     */
    Page<SavedRepo> findAllByUserIdAndIsPinnedFalseAndLanguageMainIgnoreCase(Long userId, String language, Pageable pageable);
}