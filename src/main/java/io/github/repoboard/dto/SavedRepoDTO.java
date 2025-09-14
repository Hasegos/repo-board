package io.github.repoboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SavedRepoDTO {

    private Integer repoGithubId;
    private String name;
    private String htmlUrl;
    private String description;
    private String languageMain;
    private Integer stars;
    private Integer forks;
    private String readmeExcerpt;
    private String note;
    private Instant updatedAt;

    private String ownerLogin;
    private String ownerAvatarUrl;
    private String ownerHtmlUrl;
}