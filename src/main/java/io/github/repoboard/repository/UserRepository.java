package io.github.repoboard.repository;

import io.github.repoboard.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@link User} 엔티티에 대한 JPA 리포지토리 인터페이스.
 *
 * <p>기본 CRUD 메서드 외에 사용자명 및 소셜 provider ID를 통한 조회 메서드를 제공한다.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 소셜 providerId로 사용자 조회
     *
     * @param providerId 소셜 로그인 식별자
     * @return providerId에 해당하는 사용자(Optional)
     */
    Optional<User> findByProviderId(String providerId);

    /**
     * 사용자명(username)으로 사용자 조회
     *
     * @param username 고유 사용자명
     * @return username에 해당하는 사용자(Optional)
     */
    Optional<User> findByUsername(String username);

    /**
     * 특정 사용자명이 존재하는지 확인
     *
     * @param username 확인할 사용자명
     * @return 존재 여부
     */
    boolean existsByUsername(String username);
}