package io.github.repoboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

/**
 * GitHub 사용자 프로필 정보를 담는 DTO.
 * <p>
 * GitHub REST API v3의 {@code /users/{username}} 응답을 매핑합니다.
 * </p>
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubUserDTO {

    /** GitHub 로그인 ID */
    private String login;

    /** 사용자 표시 이름 */
    private String name;

    /** 자기소개(Bio) */
    private String bio;

    /** 아바타 이미지 URL */
    @JsonProperty("avatar_url")
    private String avatarUrl;

    /** GitHub 프로필 페이지 URL */
    @JsonProperty("html_url")
    private String htmlUrl;

    /** 공개 레포지토리 개수 */
    @JsonProperty("public_repos")
    private Integer publicRepos;

    /** 블로그 주소 */
    private String blog;

    /** 팔로우 수 */
    public Integer followers;

    /** 사용자가 팔로우한 수 */
    public Integer following;
}