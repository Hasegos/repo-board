package io.github.repoboard.model;

import io.github.repoboard.model.enums.ProfileVisibility;
import io.github.repoboard.model.enums.UserProvider;
import io.github.repoboard.model.enums.UserRoleType;
import io.github.repoboard.model.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "deleted_users")
public class DeleteUser {

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "delete_at", nullable = false)
    private Instant deleteAt;

    @Column(name = "deleted_by_admin")
    private String deletedByAdmin;

    @Column(name = "github_login")
    private String githubLogin;

    @Column(name = "github_name")
    private String githubName;

    @Column(name = "github_bio")
    private String githubBio;

    @Column(name = "github_blog")
    private String githubBlog;

    @Column(name = "follower_count")
    private Integer githubFollowers;

    @Column(name = "following_count")
    private Integer githubFollowing;

    @Column(name = "github_avatar_url")
    private String githubAvatarUrl;

    @Column(name = "github_html_url")
    private String githubHtmlUrl;

    @Column(name = "github_public_repos")
    private Integer githubPublicRepos;

    @Column(name = "s3Key")
    private String s3Key;

    @Column(name = "profile_visibility")
    @Enumerated(EnumType.STRING)
    private ProfileVisibility profileVisibility;

    @Column(name = "last_refresh_at")
    private Instant lastRefreshAt;

    @Column(name = "profile_created_at", updatable = false)
    private Instant profileCreatedAt;

    @Column(name = "profile_updated_at")
    private Instant profileUpdatedAt;
}