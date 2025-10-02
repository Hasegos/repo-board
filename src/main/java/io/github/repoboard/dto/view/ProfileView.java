package io.github.repoboard.dto.view;

import io.github.repoboard.dto.github.GithubRepoDTO;
import io.github.repoboard.model.Profile;
import io.github.repoboard.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@AllArgsConstructor
public class ProfileView {
    private final User user;
    private final Profile profile;
    private final Page<GithubRepoDTO> repos;
    private final String currentType;
    private final boolean onboarding;
}