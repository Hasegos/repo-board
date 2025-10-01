package io.github.repoboard.service;

import io.github.repoboard.dto.github.GithubUserDTO;
import io.github.repoboard.model.Profile;
import io.github.repoboard.model.User;
import io.github.repoboard.model.enums.ProfileVisibility;
import io.github.repoboard.repository.ProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ProfileDBService {

    private final ProfileRepository profileRepository;
    private final UserService userService;

    @Transactional
    public void createProfileDB(Long userId,
                                   GithubUserDTO githubUserDTO,
                                   String imageUrl,
                                   String s3Key){

        User user = userService.findByUserId(userId);
        Profile profile = new Profile();
        profile.setUser(user);
        profile.setGithubLogin(githubUserDTO.getLogin());
        profile.setGithubName(githubUserDTO.getName() != null ? githubUserDTO.getName() : githubUserDTO.getLogin());
        profile.setGithubBio(githubUserDTO.getBio());
        profile.setGithubBlog(githubUserDTO.getBlog());
        profile.setGithubFollowers(githubUserDTO.getFollowers());
        profile.setGithubFollowing(githubUserDTO.getFollowing());
        profile.setGithubAvatarUrl(imageUrl);
        profile.setGithubHtmlUrl(githubUserDTO.getHtmlUrl());
        profile.setGithubPublicRepos(githubUserDTO.getPublicRepos());
        profile.setS3Key(s3Key);
        profile.setProfileVisibility(ProfileVisibility.PRIVATE);
        profile.setCreatedAt(Instant.now());

        profileRepository.save(profile);
    }

    @Transactional
    public void updateProfileDB(Long userId, GithubUserDTO dto){

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("프로필이 존재하지 않습니다."));

        profile.setGithubBio(dto.getBio());
        profile.setGithubBlog(dto.getBlog());
        profile.setGithubFollowers(dto.getFollowers());
        profile.setGithubFollowing(dto.getFollowing());
        profile.setGithubPublicRepos(dto.getPublicRepos());
        profile.setUpdatedAt(Instant.now());

        profileRepository.save(profile);
    }

    @Transactional
    public void updateProfileImageDB(Profile profile, String imageUrl, String s3Key){

        profile.setGithubAvatarUrl(imageUrl);
        profile.setS3Key(s3Key);
        profile.setUpdatedAt(Instant.now());

        profileRepository.save(profile);
    }

    /**
     * 프로필 공개 여부를 변경한다.
     *
     * @param userId 사용자 ID
     * @param visibility "PUBLIC" 또는 "PRIVATE" 값 (null 가능)
     * @throws EntityNotFoundException 프로필이 존재하지 않을 경우
     */
    @Transactional
    public void updateProfileVisibility(Long userId, String visibility){
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("등록된 프로필이 없습니다. 먼저 프로필을 생성해주세요."));

        ProfileVisibility newVisibility =
                (visibility != null && visibility.equals("PUBLIC"))
                        ? ProfileVisibility.PUBLIC
                        : ProfileVisibility.PRIVATE;

        profile.setProfileVisibility(newVisibility);
        profile.setUpdatedAt(Instant.now());
    }

    @Transactional
    public void deleteProfileDB(Long profileId){
        profileRepository.deleteById(profileId);
    }
}