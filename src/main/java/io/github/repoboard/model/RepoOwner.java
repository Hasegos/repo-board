package io.github.repoboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RepoOwner {

    @Column(name = "owner_avatar_url", nullable = false)
    private String ownerAvatarUrl;

    @Column(name = "owner_html_url", nullable = false)
    private String ownerHtmlUrl;

    @Column(name = "owner_login", nullable = false)
    private String ownerLogin;
}