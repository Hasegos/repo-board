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

        ProfileVisibility newVisibility = parseVisibility(visibility);
        profile.setProfileVisibility(newVisibility);
    }

    /**
     * 문자열 값을 {@link ProfileVisibility} Enum으로 변환한다.
     * <p>허용값: PUBLIC, PRIVATE</p>
     *
     * @param visibility 프로필 공개 상태 문자열
     * @return 변환된 {@link ProfileVisibility} 값
     * @throws IllegalArgumentException 잘못된 값일 경우
     */
    private ProfileVisibility parseVisibility(String visibility){
        if("PUBLIC".equalsIgnoreCase(visibility)){
            return ProfileVisibility.PUBLIC;
        } else if("PRIVATE".equalsIgnoreCase(visibility)){
            return ProfileVisibility.PRIVATE;
        }else{
            throw new IllegalArgumentException("올바르지 않은 공개 설정 값입니다.");
        }
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
     * 삭제된 사용자 백업 데이터를 기반으로 프로필을 복원한다.
     *
     * @param user 복구된 사용자 엔티티
     * @param d 삭제된 사용자 백업 정보
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

        profileRepository.save(p);
    }
}