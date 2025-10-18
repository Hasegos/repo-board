package io.github.repoboard.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * GitHub API의 검색 응답을 매핑하는 제네릭 DTO.
 *
 * @param <T> 검색 결과 아이템 타입
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubSearchResponse<T> {

    @JsonProperty("total_count")
    private long totalCount;

    private List<T> items;
}