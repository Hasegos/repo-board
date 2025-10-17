package io.github.repoboard.dto.view;

import io.github.repoboard.model.Profile;
import io.github.repoboard.model.enums.ProfileVisibility;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProfileFullView {

    private final String githubLogin;
    private final String githubName;
    private final String githubBio;
    private final String githubBlog;
    private final Integer githubFollowers;
    private final Integer githubFollowing;
    private final Integer githubPublicRepos;
    private final String githubAvatarUrl;
    private final String githubHtmlUrl;
    private final ProfileVisibility profileVisibility;

    public static ProfileFullView from(Profile profile){
        if(profile == null) return null;
        return new ProfileFullView(
                profile.getGithubLogin(),
                profile.getGithubName(),
                profile.getGithubBio(),
                profile.getGithubBlog(),
                profile.getGithubFollowers(),
                profile.getGithubFollowing(),
                profile.getGithubPublicRepos(),
                profile.getGithubAvatarUrl(),
                profile.getGithubHtmlUrl(),
                profile.getProfileVisibility() != null
                    ? profile.getProfileVisibility() : ProfileVisibility.PRIVATE
        );
    }
}