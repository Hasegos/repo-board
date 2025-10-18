package io.github.repoboard.dto.request;

import lombok.*;

import java.time.Instant;

/**
 * 저장된 GitHub 레포지토리 정보를 전송하는 DTO.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SavedRepoDTO {

    private Long repoGithubId;
    private String name;
    private String htmlUrl;
    private String description;
    private String language;
    private Integer stars;
    private Integer forks;
    private String readmeExcerpt;
    private String note;
    private Instant updatedAt;

    private String ownerLogin;
    private String ownerAvatarUrl;
    private String ownerHtmlUrl;
}