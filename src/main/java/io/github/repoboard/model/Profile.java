package io.github.repoboard.model;

import io.github.repoboard.common.domain.BaseTimeEntity;
import io.github.repoboard.model.enums.ProfileVisibility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * 사용자의 GitHub 프로필 정보를 저장하는 엔티티.
 *
 * <p>사용자(User)와 1:1 매핑되며, GitHub에서 가져온 정보와 오픈 프로필 공개 여부 등을 포함한다.</p>
 *
 * <h3>연관관계</h3>
 * <ul>
 *   <li>{@link User} : 사용자 정보 (1:1, PK 공유)</li>
 * </ul>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "profiles")
public class Profile extends BaseTimeEntity {

    /** 사용자 ID (User의 PK와 공유) */
    @Id
    @Column(name = "user_id")
    private Long id;

    /** 사용자 연관 엔티티 (1:1) */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** GitHub 로그인 ID */
    @Column(name = "github_login", nullable = false)
    private String githubLogin;

    /** github 닉네임 */
    @Column(name = "github_name")
    private String githubName;

    /** github 자기소개 (bio) */
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

    /** 저장된 S3 객체 키 (아바타 등) */
    @Column(name = "s3Key")
    private String s3Key;

    /** 프로필 공개 여부 (PUBLIC / PRIVATE) */
    @Column(name = "profile_visibility")
    @Enumerated(EnumType.STRING)
    private ProfileVisibility profileVisibility = ProfileVisibility.PRIVATE;

    /** 연속 클릭 방지용 (시간) */
    @Column(name = "last_refresh_at")
    private Instant lastRefreshAt;
}