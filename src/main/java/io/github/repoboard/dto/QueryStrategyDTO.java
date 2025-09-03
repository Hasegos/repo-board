package io.github.repoboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QueryStrategyDTO {

    private String query;
    private String sort;
}