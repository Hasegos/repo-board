package io.github.repoboard.model;

import io.github.repoboard.common.domain.BaseTimeEntity;
import io.github.repoboard.model.enums.UserProvider;
import io.github.repoboard.model.enums.UserRoleType;
import io.github.repoboard.model.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 애플리케이션 사용자를 나타내는 엔티티.
 *
 * <p>사용자는 일반 로그인 또는 OAuth2(GitHub/Google 등)를 통해 가입하며,</p>
 * <p>각 사용자에게는 역할(Role), 상태(Status), 생성/수정 시각 등이 포함된다.</p>
 *
 * <h3>연관 관계</h3>
 * <ul>
 *   <li>{@link Profile} : 사용자 프로필 (1:1)</li>
 *   <li>{@link SavedRepo} : 사용자가 저장한 GitHub 레포 목록 (1:N)</li>
 * </ul>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_status",
                columnList = "status")
        }
)
public class User extends BaseTimeEntity {

    /** 사용자 ID (PK) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 사용자명 (고유값, 로그인 ID) */
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    /** 암호화된 비밀번호 */
    @Column(name = "password", nullable = false)
    private String password;

    /** 사용자 역할 (예: USER, ADMIN) */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private UserRoleType role;

    /** OAuth 제공자 (예: GITHUB, GOOGLE) */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    private UserProvider provider;

    /** OAuth 제공자에서 받은 식별자 */
    @Column(name = "provider_id", unique = true)
    private String providerId;

    /** 사용자 상태 (예: ACTIVE, SUSPENDED, DELETED) */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    /** 사용자 프로필 (1:1) */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL,
             fetch = FetchType.LAZY, orphanRemoval = true)
    private Profile profile;

    /** 사용자가 저장한 레포지토리들 (1:N) */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<SavedRepo> savedRepos;
}