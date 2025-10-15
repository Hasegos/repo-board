package io.github.repoboard.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class S3DeleteEvent {

    private final String s3Key;
    private final String reason;

    public S3DeleteEvent(String s3Key) {
        this.s3Key = s3Key;
        this.reason = null;
    }
}