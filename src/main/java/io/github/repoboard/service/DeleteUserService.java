package io.github.repoboard.service;

import io.github.repoboard.common.event.UserDeletedEvent;
import io.github.repoboard.model.DeleteUser;
import io.github.repoboard.model.Profile;
import io.github.repoboard.model.User;
import io.github.repoboard.model.enums.UserStatus;
import io.github.repoboard.repository.DeleteUserRepository;
import io.github.repoboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteUserService{

    private final UserService userService;
    private final UserRepository userRepository;
    private final DeleteUserRepository deleteUserRepository;
    private final ProfileDBService profileDBService;
    private final S3Service s3Service;
    private final ApplicationEventPublisher eventPublisher;

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
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
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
        String s3key = profile != null ? profile.getS3Key() : null;

        userRepository.delete(user);
        log.warn("[ADMIN] {}가 사용자 삭제함 → username: {}, userId: {}",
                backup.getDeletedByAdmin(), user.getUsername(), user.getId());

        if (s3key != null && !s3key.isEmpty()) {
            eventPublisher.publishEvent(new UserDeletedEvent(this, s3key));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserDeleted(UserDeletedEvent event) {
        try {
            s3Service.deleteFile(event.getS3Key());
            log.info("✅ S3 삭제 성공 → {}", event.getS3Key());
        } catch (Exception e) {
            log.error("❌ S3 삭제 실패 → {}", event.getS3Key(), e);
        }
    }

    @Transactional
    public User restoreUser(DeleteUser backup) {
        if (backup.getDeleteAt().isBefore(Instant.now().minus(Duration.ofDays(7)))) {
            throw new IllegalStateException("7일이 지나 복구할 수 없습니다.");
        }

        User user = userService.createdUserFromBackup(backup);
        profileDBService.createProfileFromBackup(user, backup);

        deleteUserRepository.delete(backup);

        log.info("🟢 사용자 복구 완료 → username: {}, userId: {}", user.getUsername(), user.getId());

        return user;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void purgeOldBackups() {
        Instant threshold = Instant.now().minus(Duration.ofDays(7));
        int countBefore = deleteUserRepository.findAll().size();

        deleteUserRepository.deleteAllByDeleteAtBefore(threshold);

        int countAfter = deleteUserRepository.findAll().size();
        int removed = countBefore - countAfter;

        log.info("✅ 7일 이상 지난 삭제 백업 정리 완료 - {}건 삭제됨", removed);
    }
}