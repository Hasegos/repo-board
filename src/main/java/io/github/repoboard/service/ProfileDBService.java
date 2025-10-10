package io.github.repoboard.service;

import io.github.repoboard.dto.github.GithubUserDTO;
import io.github.repoboard.model.DeleteUser;
import io.github.repoboard.model.Profile;
import io.github.repoboard.model.User;
import io.github.repoboard.model.enums.ProfileVisibility;
import io.github.repoboard.repository.ProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 프로필 엔티티를 DB에 생성/수정/삭제하는 서비스.
 *
 * <p>비즈니스 로직보다는 영속성 관리에 집중하며,
 * UserService, ProfileRepository와 협력한다.</p>
 */
@Service
@RequiredArgsConstructor
public class ProfileDBService {

    private final ProfileRepository profileRepository;
    private final UserService userService;

    /**
     * 새 프로필을 DB에 생성한다.
     *
     * @param userId        사용자 ID
     * @param githubUserDTO GitHub 사용자 정보 DTO
     * @param imageUrl      저장된 프로필 이미지 URL
     * @param s3Key         S3 오브젝트 키
     */
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

    /**
     * 기존 프로필의 기본 정보를 업데이트한다.
     *
     * @param userId 사용자 ID
     * @param dto    GitHub 사용자 정보 DTO
     * @throws EntityNotFoundException 프로필이 존재하지 않을 경우
     */
    @Transactional
    public void updateProfileDB(Long userId, GithubUserDTO dto){

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("프로필이 존재하지 않습니다."));

        profile.setGithubLogin(dto.getLogin());
        profile.setGithubName(dto.getName());
        profile.setGithubBio(dto.getBio());
        profile.setGithubBlog(dto.getBlog());
        profile.setGithubFollowers(dto.getFollowers());
        profile.setGithubFollowing(dto.getFollowing());
        profile.setGithubPublicRepos(dto.getPublicRepos());
        profile.setUpdatedAt(Instant.now());

        profileRepository.save(profile);
    }

    /**
     * 프로필 이미지와 S3 키를 업데이트한다.
     *
     * @param profile  수정할 프로필 엔티티
     * @param imageUrl 새 프로필 이미지 URL
     * @param s3Key    새 S3 오브젝트 키
     */
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

    /**
     * 프로필을 DB에서 삭제한다.
     *
     * @param profileId 삭제할 프로필 ID
     */
    @Transactional
    public void deleteProfileDB(Long profileId){
        profileRepository.deleteById(profileId);
    }

    /**
     *
     * @param user
     * @param d
     */
    public void createProfileFromBackup(User user, DeleteUser d) {
        Profile p = new Profile();
        p.setUser(user);
        p.setGithubLogin(d.getGithubLogin());
        p.setGithubName(d.getGithubName());
        p.setGithubBio(d.getGithubBio());
        p.setGithubBlog(d.getGithubBlog());
        p.setGithubFollowers(d.getGithubFollowers());
        p.setGithubFollowing(d.getGithubFollowing());
        p.setGithubAvatarUrl(d.getGithubAvatarUrl());
        p.setGithubHtmlUrl(d.getGithubHtmlUrl());
        p.setGithubPublicRepos(d.getGithubPublicRepos());
        p.setS3Key(d.getS3Key());
        p.setProfileVisibility(d.getProfileVisibility());
        p.setLastRefreshAt(d.getLastRefreshAt());
        p.setCreatedAt(d.getProfileCreatedAt());
        p.setUpdatedAt(Instant.now());

        profileRepository.save(p);
    }
}