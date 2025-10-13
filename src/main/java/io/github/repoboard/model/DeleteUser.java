package io.github.repoboard.model;

import io.github.repoboard.model.enums.ProfileVisibility;
import io.github.repoboard.model.enums.UserProvider;
import io.github.repoboard.model.enums.UserRoleType;
import io.github.repoboard.model.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 삭제된 사용자 및 프로필 정보를 백업 저장하는 엔티티.
 *
 * <p>탈퇴 또는 관리자에 의해 삭제된 사용자의 정보를 일정 기간 보관하기 위해 사용된다.</p>
 * <p>복구 또는 중복 가입 방지를 위한 용도로 활용된다.</p>
 *
 * <h3>주요 정보</h3>
 * <ul>
 *   <li>기본 회원 정보 (username, provider 등)</li>
 *   <li>GitHub 프로필 정보</li>
 *   <li>삭제 시각 및 관리자 정보</li>
 *   <li>S3 이미지 경로 등</li>
 * </ul>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "deleted_users")
public class DeleteUser {

    /** 삭제 사용자 백업 ID (PK) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 사용자명 (고유값, 로그인 ID) */
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    /** 암호화된 비밀번호 */
    @Column(name = "password", nullable = false)
    private String password;

    /** 사용자 권한 (예: USER, ADMIN) */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private UserRoleType role;

    /** OAuth 제공자 (예: GITHUB, GOOGLE) */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    private UserProvider provider;

    /** OAuth 제공자 식별자 */
    @Column(name = "provider_id", unique = true)
    private String providerId;

    /** 사용자 상태 (예: DELETED, SUSPENDED) */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    /** 가입 시각 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** 마지막 수정 시각 */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** 삭제 시각 */
    @Column(name = "delete_at", nullable = false)
    private Instant deleteAt;

    /** 관리자 삭제 시, 관리자 이름 */
    @Column(name = "deleted_by_admin")
    private String deletedByAdmin;

    /** GitHub 로그인 ID */
    @Column(name = "github_login")
    private String githubLogin;

    /** GitHub 닉네임 */
    @Column(name = "github_name")
    private String githubName;

    /** GitHub 소개글 (bio) */
    @Column(name = "github_bio")
    private String githubBio;

    /** GitHub 블로그 주소 */
    @Column(name = "github_blog")
    private String githubBlog;

    /** GitHub 팔로워 수 */
    @Column(name = "follower_count")
    private Integer githubFollowers;

    /** GitHub 팔로잉 수 */
    @Column(name = "following_count")
    private Integer githubFollowing;

    /** GitHub 아바타 이미지 URL */
    @Column(name = "github_avatar_url")
    private String githubAvatarUrl;

    /** GitHub 프로필 페이지 URL */
    @Column(name = "github_html_url")
    private String githubHtmlUrl;

    /** GitHub 공개 레포지토리 수 */
    @Column(name = "github_public_repos")
    private Integer githubPublicRepos;

    /** S3에 저장된 프로필 이미지 키 */
    @Column(name = "s3Key")
    private String s3Key;

    /** 오픈 프로필 공개 여부 */
    @Column(name = "profile_visibility")
    @Enumerated(EnumType.STRING)
    private ProfileVisibility profileVisibility;

    /** 마지막 프로필 새로고침 시각 */
    @Column(name = "last_refresh_at")
    private Instant lastRefreshAt;

    /** 프로필 생성 시각 */
    @Column(name = "profile_created_at", updatable = false)
    private Instant profileCreatedAt;

    /** 프로필 수정 시각 */
    @Column(name = "profile_updated_at")
    private Instant profileUpdatedAt;
}