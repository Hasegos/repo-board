package io.github.repoboard.model;

import io.github.repoboard.model.enums.ProfileVisibility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "profiles")
public class Profile {

    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** github 로그인 명 */
    @Column(name = "github_login")
    private String githubLogin;

    /** github 닉네임 */
    @Column(name = "github_name", nullable = false)
    private String githubName;

    /** github 자기소개 */
    @Column(name = "github_bio")
    private String githubBio;

    /** github 본인 블로그 주소 */
    @Column(name = "github_blog")
    private String githubBlog;

    /** github follower 수 */
    @Column(name = "follower_count")
    private Integer githubFollowers;

    /** github following 수 */
    @Column(name = "following_count")
    private Integer githubFollowing;

    /** github 이미지 주소 */
    @Column(name = "github_avatar_url")
    private String githubAvatarUrl;

    /** github 본인 repo주소 */
    @Column(name = "github_html_url")
    private String githubHtmlUrl;
    
    /** github 공개된 Repo 수 */
    @Column(name = "github_public_repos")
    private Integer githubPublicRepos;

    @Column(name = "s3Key")
    private String s3Key;

    /** 오픈프로필 공개 여부 */
    @Column(name = "profile_visibility")
    @Enumerated(EnumType.STRING)
    private ProfileVisibility profileVisibility = ProfileVisibility.PRIVATE;

    /** 연속 클릭 방지용 (시간) */
    @Column(name = "last_refresh_at")
    private Instant lastRefreshAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}