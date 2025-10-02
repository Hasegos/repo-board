package io.github.repoboard.dto.view;

import io.github.repoboard.model.SavedRepo;
import io.github.repoboard.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.Set;

@AllArgsConstructor
@Getter
public class SavedRepoView {
    private final User user;
    private final Page<SavedRepo> pinnedRepos;
    private final Page<SavedRepo> unpinnedRepos;
    private final Set<String> languageOptions;
}