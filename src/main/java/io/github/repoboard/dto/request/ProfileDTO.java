package io.github.repoboard.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileDTO {

    private String githubBio;
    private String githubBlog;
    private String githubAvatarUrl;
}