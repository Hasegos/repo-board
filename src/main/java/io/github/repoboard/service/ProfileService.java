package io.github.repoboard.service;

import io.github.repoboard.common.event.S3DeleteEvent;
import io.github.repoboard.dto.github.GithubRepoDTO;
import io.github.repoboard.dto.github.GithubUserDTO;
import io.github.repoboard.dto.view.ProfileView;
import io.github.repoboard.model.Profile;
import io.github.repoboard.model.User;
import io.github.repoboard.model.enums.ProfileVisibility;
import io.github.repoboard.repository.ProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 서비스 레이어 클래스.
 *
 * <p>사용자의 GitHub 프로필 관리(등록, 조회, 새로고침, 삭제 등)와
 * GitHub API, S3 저장소, DB 간의 연동을 담당한다.</p>
 *
 * 주요 기능:
 * <ul>
 *     <li>프로필 등록 및 삭제</li>
 *     <li>프로필 조회 및 GitHub API 연동</li>
 *     <li>S3 기반 프로필 이미지 업로드 및 관리</li>
 *     <li>GitHub 저장소 목록 조회 및 필터링</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserService userService;
    private final ProfileRepository profileRepository;
    private final ProfileDBService profileDBService;
    private final GitHubApiService gitHubApiService;
    private final S3Service s3Service;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 특정 사용자에 대해 프로필이 이미 존재하는지 확인한다.
     *
     * @param userId 사용자 ID
     * @throws IllegalArgumentException 이미 프로필이 존재할 경우 예외 발생
     */
    @Transactional(readOnly = true)
    public void ensureProfileNotExists(Long userId){
        if(profileRepository.existsByUserId(userId)){
            throw new IllegalArgumentException("이미 프로필이 존재합니다.");
        }
    }

    /**
     * GitHub 로그인명으로 프로필 조회.
     *
     * @param githubLogin GitHub 로그인명
     * @return 해당 로그인명의 {@link Profile} Optional
     */
    @Transactional(readOnly = true)
    public Optional<Profile> findProfileByGithubLogin(String githubLogin){
        return profileRepository.findByGithubLoginAndProfileVisibility(githubLogin, ProfileVisibility.PUBLIC);
    }

    /**
     * 사용자 ID로 프로필 조회.
     *
     * @param userId 사용자 ID
     * @return 해당 사용자의 {@link Profile} Optional
     */
    @Transactional(readOnly = true)
    public Optional<Profile> findProfileByUserId(Long userId){
        return profileRepository.findByUserId(userId);
    }

    /**
     * 이미지 URL에서 S3 key 추출.
     *
     * @param imageUrl S3 업로드된 이미지 URL
     * @return S3 key
     * @throws IllegalArgumentException 잘못된 URL 형식인 경우
     */
    private String extractS3KeyFromUrl(String imageUrl){

        if(imageUrl == null || !imageUrl.contains("/")){
            throw new IllegalArgumentException("올바른 이미지 URL이 아닙니다.");
        }
        try{
            var uri = java.net.URI.create(imageUrl);
            String path = uri.getPath();
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e){
            throw new IllegalArgumentException("올바른 이미지 URL이 아닙니다.");
        }
    }

    /**
     * GitHub 저장소 목록 로드 및 타입별 필터링.
     *
     * @param username GitHub 사용자명
     * @param pageable 페이지 정보
     * @param type 필터 타입 (all, original, fork)
     * @return 필터링된 저장소 페이지
     */
    @Transactional(readOnly = true)
    public Page<GithubRepoDTO> loadProfileView(String username,
                                               Pageable pageable,
                                               String type){
        Page<GithubRepoDTO> reposPage = gitHubApiService.getOwnedRepos(username, pageable);
        List<GithubRepoDTO> filterContent = reposPage.getContent().stream()
                .filter(repo -> switch (type) {
                    case "original" -> !repo.getFork();
                    case "fork" -> repo.getFork();
                    default -> true;
                }).collect(Collectors.toList());

        return new PageImpl<>(filterContent, pageable, reposPage.getTotalElements());
    }

    /**
     * 새 프로필을 등록한다.
     * <p>DB와 S3 양쪽에 반영되며, 실패 시 롤백 처리한다.</p>
     *
     * @param userId 사용자 ID
     * @param dto GitHub 사용자 DTO
     * @return 생성된 {@link Profile}
     * @throws IOException 이미지 업로드 실패 시
     */
    public void registerProfile(Long userId, GithubUserDTO dto) throws IOException{
        ensureProfileNotExists(userId);

        String imageUrl = null;
        String s3Key = null;

        try{
            imageUrl = s3Service.uploadFromUrl(dto.getAvatarUrl());
            s3Key = extractS3KeyFromUrl(imageUrl);
        } catch (IOException e){
            throw new IOException("프로필 이미지 업로드 실패 : " + e);
        }

        try{
            profileDBService.createProfileDB(userId, dto, imageUrl, s3Key);
            log.info("[PROFILE] 사용자 {} 프로필 등록 완료", userId);
        }catch (Exception e){
            if(s3Key != null && !s3Key.isEmpty()) {
                try{
                    s3Service.deleteFile(s3Key);
                }catch (Exception ex){
                    log.error("롤백 중 S3 파일 삭제 실패 : key = {}" , s3Key, ex);
                }
            }
            throw e;
        }
    }

    /**
     * 프로필 이미지를 최신화한다.
     * <p>S3와 DB에 모두 반영하며, 실패 시 롤백 처리.</p>
     *
     * @param profile 프로필 엔티티
     * @param githubAvatarUrl 새로운 GitHub 아바타 URL
     * @throws IOException 이미지 업로드 실패 시
     */
    public void refreshProfileImage(Profile profile, String githubAvatarUrl) throws IOException {

        String oldKey = profile.getS3Key();
        String newImageUrl = null;
        String newS3Key = null;

        try{
            newImageUrl = s3Service.uploadFromUrl(githubAvatarUrl);
            newS3Key = extractS3KeyFromUrl(newImageUrl);
        } catch (Exception e){
            throw new IOException("프로필 이미지 업로드 실패 : " + e);
        }

        try{
            profileDBService.updateProfileImageDB(profile, newImageUrl, newS3Key);
            log.info("[PROFILE] 사용자 {} 프로필 이미지 갱신 완료", profile.getUser().getId());
        }catch (Exception e){
            try{
                s3Service.deleteFile(newS3Key);
            } catch (Exception ex){
                log.error("롤백 중 S3 파일 삭제 실패 : key = {}", newS3Key, ex);
            }
            throw e;
        }

        if(oldKey != null && !oldKey.isEmpty()) {
            eventPublisher.publishEvent(new S3DeleteEvent(oldKey, "profile-image-replaced"));
        }
    }

    /**
     * 프로필 페이지 데이터를 로드한다.
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param type 필터 타입 (all, original, fork)
     * @return {@link ProfileView} 객체
     */
    @Transactional(readOnly = true)
    public ProfileView loadProfilePage(Long userId, int page, int size, String type){
        User user = userService.findByUserId(userId);

        Optional<Profile> profileOpt = findProfileByUserId(user.getId());
        if(profileOpt.isEmpty()){
            return ProfileView.of(user, null, Page.empty(), type, true);
        }

        Profile profile = profileOpt.get();
        Pageable pageable = PageRequest.of(page,size);
        Page<GithubRepoDTO> reposPage = loadProfileView(profile.getGithubLogin(), pageable, type);

        return ProfileView.of(user, profile, reposPage, type, false);
    }

    /**
     * 프로필 새로고침.
     * <p>GitHub API에서 최신 정보를 불러와 DB 및 이미지를 갱신한다.</p>
     *
     * @param userId 사용자 ID
     * @throws IOException 이미지 업로드 실패 시
     * @throws EntityNotFoundException 등록된 프로필이 없는 경우
     * @throws IllegalArgumentException 새로고침 쿨다운(10초) 미준수 시
     */
    @Transactional
    public void refreshProfile(Long userId) throws IOException {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("등록된 프로필이 없습니다. 먼저 프로필을 생성해주세요."));

        if (profile.getLastRefreshAt() != null &&
                Duration.between(profile.getLastRefreshAt(), Instant.now()).toMinutes() < 3) {
            log.warn("[PROFILE] 사용자 {} 가 쿨다운(3분) 미준수로 새로고침 시도", userId);
            throw new IllegalArgumentException("새로고침은 3분에 한 번만 가능합니다.");
        }

        GithubUserDTO userDTO = gitHubApiService.refreshUser(profile.getGithubLogin());
        profileDBService.updateProfileDB(userId, userDTO);
        refreshProfileImage(profile, userDTO.getAvatarUrl());

        profile.setLastRefreshAt(Instant.now());
        log.info("[PROFILE] 사용자 {} 프로필 새로고침 완료", userId);
    }

    /**
     * GitHub 프로필 등록 (최초 세팅).
     *
     * @param userId 사용자 ID
     * @param url GitHub 프로필 URL
     * @throws IOException GitHub API 요청 실패 또는 이미지 업로드 실패 시
     */
    @Transactional
    public void setupProfile(Long userId, String url) throws IOException{
        String username = gitHubApiService.extractUsername(url);
        GithubUserDTO userDTO = gitHubApiService.getUser(username);

        if(findProfileByUserId(userId).isEmpty()){
            registerProfile(userId, userDTO);
        }
    }

    /**
     * 사용자 ID로 프로필 삭제.
     * <p>DB에서 삭제 후 S3 파일도 함께 삭제한다.</p>
     *
     * @param userId 사용자 ID
     */
    @Transactional
    public void deleteProfileByUserId(Long userId){

       Optional<Profile> profileOptional = findProfileByUserId(userId);

       if(profileOptional.isEmpty()){
           log.info("[PROFILE] 사용자 {} 프로필 삭제 요청 → 존재하지 않음", userId);
           return;
       }

       Profile profile = profileOptional.get();
       String s3Key = profile.getS3Key();
       profileDBService.deleteProfileDB(profile.getId());
       log.info("[PROFILE] 사용자 {} 프로필 DB 삭제 완료", userId);

       if(s3Key != null && !s3Key.isEmpty()){
           eventPublisher.publishEvent(new S3DeleteEvent(s3Key, "profile-deleted"));
       }
    }
}