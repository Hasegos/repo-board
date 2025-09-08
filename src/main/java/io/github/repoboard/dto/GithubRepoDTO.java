package io.github.repoboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

/**
 * GitHub 레포지토리 정보를 담는 DTO.
 * <p>
 * GitHub REST API v3의 {@code /users/{username}/repos} 응답을 매핑합니다.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubRepoDTO {

    /** 레포지토리 고유 ID */
    private Long id;

    /** 레포지토리 단축 이름 */
    private String name;

    /** 소유자/레포지토리 형태의 전체 이름 */
    @JsonProperty("full_name")
    private String fullName;

    /** 레포지토리 설명 */
    private String description;

    /** 주 프로그래밍 언어 */
    private String language;

    /** 레포지토리 웹 페이지 URL */
    @JsonProperty("html_url")
    private String htmlUrl;

    /** 별(star) 수 */
    @JsonProperty("stargazers_count")
    private Integer stargazersCount;

    /** 포크(fork) 수 */
    @JsonProperty("forks_count")
    private Integer forksCount;

    /** 포크 여부 */
    private Boolean fork;

    /** 메타데이터가 수정된 날짜 */
    @JsonProperty("updated_at")
    private Instant updatedAt;
}