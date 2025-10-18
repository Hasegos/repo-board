package io.github.repoboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GitHub 레포지토리의 소유자 정보를 나타내는 임베디드 값 객체.
 *
 * <p>{@link SavedRepo} 엔티티에 포함되어 저장되며,</p>
 * <p>소유자의 로그인명, 아바타 URL, GitHub 프로필 URL을 포함한다.</p>
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RepoOwner {

    /** 소유자의 GitHub 아바타 이미지 URL */
    @Column(name = "owner_avatar_url", nullable = false)
    private String ownerAvatarUrl;

    /** 소유자의 GitHub 프로필 페이지 URL */
    @Column(name = "owner_html_url", nullable = false)
    private String ownerHtmlUrl;

    /** 소유자의 GitHub 로그인명 */
    @Column(name = "owner_login", nullable = false)
    private String ownerLogin;
}