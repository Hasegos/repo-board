package io.github.repoboard.model;

import io.github.repoboard.model.enums.UserProvider;
import io.github.repoboard.model.enums.UserRoleType;
import io.github.repoboard.model.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.util.List;

/**
 * 회원 정보를 가진 엔터티(entity) 클래스
 * <p>
 * 해당 클래스는 데이터베이스의 users 테이블과 매핑되며,<br>
 * 사용자의 로그인 정보, 권한, OAuth 제공자, 생성일 등을 포함합니다.
 * </p>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private UserRoleType role;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    private UserProvider provider;

    @Column(name = "provider_id", unique = true)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /* === EDK 연관 관계 === */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL,
             fetch = FetchType.LAZY, orphanRemoval = true)
    private Profile profile;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<SavedRepo> savedRepos;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Reaction> reactions;
}