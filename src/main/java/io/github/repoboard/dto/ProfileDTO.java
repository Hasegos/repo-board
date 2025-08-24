package io.github.repoboard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileDTO {

    private Long id;
    private String nickname;
    private String repositoryUrl;
    private String stacks;
    private String selfInfo;
    private String experience;
    private String imageUrl;
}