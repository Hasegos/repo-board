package io.github.repoboard.repository;

import io.github.repoboard.model.DeleteUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * {@link DeleteUser} 엔티티를 관리하는 JPA 리포지토리.
 *
 * <p>삭제된 사용자(백업된 사용자) 데이터를 조회하거나 정리하는 데 사용된다.</p>
 */
@Repository
public interface DeleteUserRepository extends JpaRepository<DeleteUser, Long> {

    /**
     * 사용자명으로 삭제된 사용자 정보를 조회한다.
     *
     * @param username 사용자명
     * @return 해당 사용자명의 삭제 백업(Optional)
     */
    Optional<DeleteUser> findByUsername(String username);

    /**
     * 지정한 시점 이전에 삭제된 사용자 목록을 조회한다.
     * <p>스케줄러에 의해 오래된 백업 데이터를 정리할 때 사용된다.</p>
     *
     * @param threshold 기준 시각
     * @return 기준 시각 이전에 삭제된 사용자 목록
     */
    List<DeleteUser> findAllByDeleteAtBefore(Instant threshold);

    /**
     * 소셜 provider ID로 삭제된 사용자 정보를 조회한다.
     *
     * @param providerId 소셜 로그인 식별자
     * @return 해당 providerId의 삭제 사용자(Optional)
     */
    Optional<DeleteUser> findByProviderId(String providerId);
}