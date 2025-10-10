package io.github.repoboard.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserDeletedEvent extends ApplicationEvent {

    private final String s3Key;

    public UserDeletedEvent(Object source, String s3Key) {
        super(source);
        this.s3Key = s3Key;
    }
}