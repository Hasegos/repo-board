package io.github.repoboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 만료된 DeleteUser 백업을 주기적으로 정리하는 스케줄러.
 *
 * <p>중요: 이 클래스는 트랜잭션을 열지 않는다. 단지 트리거 역할만 하며,
 * 실제 DB 삭제(+ AFTER_COMMIT S3 삭제 이벤트 발행)는
 * {@link DeleteUserService#purgeExpiredBackups()} 에서 트랜잭션으로 처리된다.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeleteUserPurgeJob {

    private final DeleteUserService deleteUserService;

    /**
     * 매일 새벽 3시에 만료 백업 정리를 트리거한다.
     */
    @Scheduled(cron = "${app.delete-user.purge-count}")
    public void run() {
         deleteUserService.purgeExpiredBackups();
    }
}