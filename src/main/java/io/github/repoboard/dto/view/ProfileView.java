package io.github.repoboard.dto.view;

import io.github.repoboard.dto.github.GithubRepoDTO;
import io.github.repoboard.model.Profile;
import io.github.repoboard.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

/**
 * 프로필 페이지 뷰 모델 DTO.
 *
 * <p>사용자, 프로필, GitHub 레포 목록, 화면 상태 정보를 포함한다.</p>
 */
@Getter
@AllArgsConstructor
public class ProfileView {

    private final UserView user;
    private final ProfileFullView profile;
    private final Page<GithubRepoDTO> repos;
    private final String currentType;
    private final boolean onboarding;

    public static ProfileView of(User user, Profile profile,
                                 Page<GithubRepoDTO> repos,
                                 String currentType,
                                 boolean onboarding){
        return new ProfileView(
                UserView.from(user),
                profile != null ? ProfileFullView.from(profile) : null,
                repos,
                currentType,
                onboarding
        );
    }
}