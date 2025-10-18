package io.github.repoboard.common.exception;

import lombok.Getter;

/**
 * GitHub API 호출 시 Rate Limit(요청 제한)에 걸렸을 때 발생하는 예외.
 * <p>
 * GitHub API는 계정 및 토큰별로 초당 요청 수와 시간당 요청 수를 제한한다.<br>
 * 이 예외는 X-RateLimit-Reset 헤더를 분석하여 API 제한이 해제되는 시각 정보를 함께 제공한다.
 * </p>
 */
@Getter
public class GithubRateLimitException extends RuntimeException{
    private final long resetTimeStamp;

    public GithubRateLimitException(String message, long resetTimeStamp) {
        super(message);
        this.resetTimeStamp = resetTimeStamp;
    }
}