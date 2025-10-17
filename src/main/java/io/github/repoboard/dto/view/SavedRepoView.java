package io.github.repoboard.dto.view;

import io.github.repoboard.model.SavedRepo;
import io.github.repoboard.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.Set;

/**
 * "저장한 레포지토리" 페이지에 표시될 데이터를 담는 뷰 DTO.
 *
 * <p>핀된 레포지토리, 일반 레포지토리, 언어 필터 목록을 포함한다.</p>
 */
@AllArgsConstructor
@Getter
public class SavedRepoView {

    private final UserView user;
    private final Page<SavedRepo> pinnedRepos;
    private final Page<SavedRepo> unpinnedRepos;
    private final Set<String> languageOptions;

    public static SavedRepoView of(User user,
                                   Page<SavedRepo> pinnedRepos,
                                   Page<SavedRepo> unpinnedRepos,
                                  Set<String> languageOptions){
        return new SavedRepoView(
                UserView.from(user),
                pinnedRepos != null ? pinnedRepos : Page.empty(),
                unpinnedRepos != null ? unpinnedRepos : Page.empty(),
                languageOptions
        );
    }
}