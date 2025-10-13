package io.github.repoboard.dto.strategy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GitHub 검색 전략을 정의하는 DTO.
 *
 * <p>검색 쿼리(query)와 정렬 기준(sort)을 함께 보유한다.</p>
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class QueryStrategyDTO {

    private String query;
    private String sort;
}