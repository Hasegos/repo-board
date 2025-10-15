package io.github.repoboard.service;

import io.github.repoboard.model.DeleteUser;
import io.github.repoboard.model.Profile;
import io.github.repoboard.model.User;
import io.github.repoboard.model.enums.UserStatus;
import io.github.repoboard.repository.DeleteUserRepository;
import io.github.repoboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 사용자 삭제 관련 기능을 처리하는 서비스 클래스.
 *
 * <p>주요 기능:</p>
 * <ul>
 *     <li>사용자 삭제 시 {@link DeleteUser} 테이블로 백업</li>
 *     <li>복구 요청 시 사용자 및 프로필 데이터 복원</li>
 *     <li>7일 이상 지난 백업과 S3 이미지 정리</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteUserService{

    private final UserService userService;
    private final UserRepository userRepository;
    private final DeleteUserRepository deleteUserRepository;
    private final ProfileDBService profileDBService;
    private final S3Service s3Service;

    @Value("${app.delete-user.retention-days}")
    private int retentionDays;

    /**
     * 사용자를 {@link DeleteUser} 테이블로 백업한 후 완전히 삭제한다.
     * <p>백업된 데이터는 7일 이내 복구 가능하며, S3 이미지는 즉시 삭제되지 않음.</p>
     *
     * @param userId 삭제할 사용자 ID
     */
    @Transactional
    public void backupAndDelete(Long userId) {
        User user = userService.findByUserId(userId);
        Profile profile = user.getProfile();

        DeleteUser backup = DeleteUser.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .role(user.getRole())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .status(UserStatus.DELETED)
                .deleteAt(Instant.now())
                .deletedByAdmin(SecurityContextHolder.getContext().getAuthentication().getName())
                .githubLogin(profile != null ? profile.getGithubLogin() : null)
                .githubName(profile != null ? profile.getGithubName() : null)
                .githubBio(profile != null ? profile.getGithubBio() : null)
                .githubBlog(profile != null ? profile.getGithubBlog() : null)
                .githubFollowers(profile != null ? profile.getGithubFollowers() : null)
                .githubFollowing(profile != null ? profile.getGithubFollowing() : null)
                .githubAvatarUrl(profile != null ? profile.getGithubAvatarUrl() : null)
                .githubHtmlUrl(profile != null ? profile.getGithubHtmlUrl() : null)
                .githubPublicRepos(profile != null ? profile.getGithubPublicRepos() : null)
                .s3Key(profile != null ? profile.getS3Key() : null)
                .profileVisibility(profile != null ? profile.getProfileVisibility() : null)
                .lastRefreshAt(profile != null ? profile.getLastRefreshAt() : null)
                .profileCreatedAt(profile != null ? profile.getCreatedAt() : null)
                .profileUpdatedAt(profile != null ? profile.getUpdatedAt() : null)
                .build();

        deleteUserRepository.save(backup);
        userRepository.delete(user);

        log.warn("[ADMIN] {}가 사용자 삭제함 → username: {}, userId: {}",
                backup.getDeletedByAdmin(), user.getUsername(), user.getId());
    }

    /**
     * {@link DeleteUser} 백업 데이터를 기반으로 사용자 및 프로필을 복구한다.
     * <p>복구는 삭제 후 7일 이내에만 가능하다.</p>
     *
     * @param backup 복구할 사용자 백업 정보
     * @return 복구된 사용자 엔티티
     * @throws IllegalStateException 삭제된 지 7일이 지난 경우
     */
    @Transactional
    public User restoreUser(DeleteUser backup) {
        if (backup.getDeleteAt().isBefore(Instant.now().minus(Duration.ofDays(retentionDays)))) {
            throw new IllegalStateException("7일이 지나 복구할 수 없습니다.");
        }

        User user = userService.createdUserFromBackup(backup);
        profileDBService.createProfileFromBackup(user, backup);

        deleteUserRepository.delete(backup);
        log.info("🟢 사용자 복구 완료 → username: {}, userId: {}", user.getUsername(), user.getId());

        return user;
    }

    /**
     * 매일 새벽 3시에 실행되어 7일 이상 지난 {@link DeleteUser} 백업 데이터와
     * 관련 S3 이미지를 정리한다.
     *
     * <p>삭제 조건: {@code deleteAt < 현재 시각 - 7일 }</p>
     */
    @Scheduled(cron = "${app.delete-user.purge-count}")
    public void purgeOldBackups() {
        Instant threshold = Instant.now().minus(Duration.ofDays(retentionDays));
        List<DeleteUser> expired = deleteUserRepository.findAllByDeleteAtBefore(threshold);

        if(expired.isEmpty()){
            log.info("✅ 삭제할 백업 없음 (기준: {})", threshold);
            return;
        }
        int s3Deleted = deleteS3Files(expired);
        deleteExpiredUsers(expired);
        log.warn("🧹 {}건 삭제 완료 (S3 {}건 포함, 기준: {})",
                expired.size(), s3Deleted, threshold);
    }

    /**
     * 만료된 삭제 사용자들의 S3 파일을 삭제한다.
     * <p>
     * 삭제 중 일부 실패해도 전체 작업은 계속 진행된다.
     *
     * @param expired 삭제 대상 사용자 목록
     * @return 성공적으로 삭제된 S3 파일 수
     */
    private int deleteS3Files(List<DeleteUser> expired) {
        int s3Deleted = 0;
        for (DeleteUser user : expired) {
            String s3Key = user.getS3Key();
            if (s3Key != null && !s3Key.isBlank()) {
                try {
                    s3Service.deleteFile(s3Key);
                    s3Deleted++;
                    log.info("🗑️ 7일 경과된 S3 이미지 삭제 완료 → {}", s3Key);
                } catch (Exception e) {
                    log.error("❌ S3 이미지 삭제 실패 → {}", s3Key, e);
                }
            }
        }
        return s3Deleted;
    }

    /**
     * 만료된 사용자 백업 데이터를 DB에서 완전히 삭제한다.
     *
     * @param expired 삭제 대상 사용자 목록
     */
    @Transactional
    public void deleteExpiredUsers(List<DeleteUser> expired) {
        deleteUserRepository.deleteAll(expired);
    }
}