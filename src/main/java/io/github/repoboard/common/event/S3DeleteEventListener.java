package io.github.repoboard.common.event;

import io.github.repoboard.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3DeleteEventListener {

    private final S3Service s3Service;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAfterCommit(S3DeleteEvent event){
        String key = event.getS3Key();
        if(key == null || key.isBlank()){
            return;
        }
        try {
            s3Service.deleteFile(key);
            log.info("🗑️ S3 삭제 완료 key={}{}", key,
                    event.getReason() != null ? " (reason=" + event.getReason() + ")" : "");
        }catch (Exception e){
            log.error("❌ S3 삭제 실패 key={}", key, e);
        }
    }
}
