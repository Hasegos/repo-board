package io.github.repoboard.service;

import io.github.repoboard.dto.GithubUserDTO;
import io.github.repoboard.dto.ProfileDTO;
import io.github.repoboard.model.Profile;
import io.github.repoboard.model.User;
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

    @Transactional
    public Profile createProfileDB(User user,
                                   GithubUserDTO githubUserDTO,
                                   String imageUrl,
                                   String s3Key){

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
        profile.setCreatedAt(Instant.now());

        return profileRepository.save(profile);
    }

    @Transactional
    public Profile updateProfileDB(Long userId, ProfileDTO profileDTO){

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("프로필이 존재하지 않습니다."));

        profile.setGithubBio(profileDTO.getGithubBio());
        profile.setGithubBlog(profileDTO.getGithubBlog());
        profile.setGithubAvatarUrl(profileDTO.getGithubAvatarUrl());
        profile.setUpdatedAt(Instant.now());

        return profileRepository.save(profile);
    }

    @Transactional
    public void updateProfileImageDB(Profile profile, String imageUrl, String s3Key){

        profile.setGithubAvatarUrl(imageUrl);
        profile.setS3Key(s3Key);
        profile.setUpdatedAt(Instant.now());

        profileRepository.save(profile);
    }

    @Transactional
    public void deleteProfileDB(Long profileId){
        profileRepository.deleteById(profileId);
    }
}