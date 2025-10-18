package io.github.repoboard.repository;

import io.github.repoboard.model.Profile;
import io.github.repoboard.model.enums.ProfileVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@link Profile} 엔티티의 CRUD 및 조회를 담당하는 JPA 리포지토리.
 *
 * <p>사용자 ID 또는 GitHub 로그인 이름으로 프로필을 조회할 수 있으며,
 * 프로필 공개 여부에 따른 조건부 조회도 지원한다.</p>
 */
@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    /**
     * 사용자 ID로 프로필을 조회한다.
     *
     * @param userId 사용자 식별자
     * @return 해당 사용자 ID의 프로필
     */
    Optional<Profile> findByUserId(Long userId);

    /**
     * GitHub 로그인과 프로필 공개 상태로 프로필을 조회한다.
     *
     * @param githubLogin GitHub 로그인 이름
     * @param profileVisibility 프로필 공개 상태
     * @return 조건에 맞는 프로필
     */
    Optional<Profile> findByGithubLoginAndProfileVisibility(String githubLogin, ProfileVisibility profileVisibility);

    /**
     * 주어진 사용자 ID에 해당하는 프로필이 존재하는지 여부를 확인한다.
     *
     * @param userId 사용자 식별자
     * @return 존재 여부
     */
    boolean existsByUserId(Long userId);
}