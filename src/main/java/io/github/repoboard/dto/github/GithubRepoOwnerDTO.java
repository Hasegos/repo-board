package io.github.repoboard.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * GitHub 레포지토리 소유자 정보를 표현하는 DTO.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubRepoOwnerDTO {

    /** 소유자 로그인 이름 */
    private String login;

    /** 소유자 이미지 URL */
    @JsonProperty("avatar_url")
    private String avatarUrl;

    /** 소유자 웹 페이지 URL */
    @JsonProperty("html_url")
    private String htmlUrl;
}