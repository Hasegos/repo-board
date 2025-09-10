package io.github.repoboard.common.handler;

import io.github.repoboard.common.exception.GithubRateLimitException;
import io.github.repoboard.common.exception.UnexpectedContentTypeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * 전역 예외 처리기(Global Exception Handler).
 * <p>
 * 컨트롤러 계층에서 발생하는 예외를 한 곳에서 처리하여
 * 일관된 JSON 응답 형식을 제공한다. <br>
 * 커스텀 예외(GithubRateLimitException, UnexpectedContentTypeException)
 * 들을 처리한다.
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * GitHub API Rate Limit 예외 처리
     *
     * @param ex GithubRateLimitException
     * @return HTTP 429 Too Many Requests + 제한 해제 시간 정보
     */
    @ExceptionHandler(GithubRateLimitException.class)
    public ResponseEntity<?> handleGithubRateLimit(GithubRateLimitException ex){
        long waitSeconds = ex.getResetTimeStamp() - Instant.now().getEpochSecond();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                        "error", "Github API 요청제한",
                        "retryAfterSeconds", waitSeconds,
                        "resetAt", Instant.ofEpochSecond(ex.getResetTimeStamp()).toString()
                ));
    }

    /**
     * 예기치 못한 Content-Type 응답 처리
     *
     * @param ex UnexpectedContentTypeException
     * @return HTTP 502 Bad Gateway + Content-Type 정보
     */
    @ExceptionHandler(UnexpectedContentTypeException.class)
    public ResponseEntity<?> handleUnexceptedContentType(UnexpectedContentTypeException ex){
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "error", "Github API로부터 예상치 못한 Content-Type 응답",
                        "contentType", ex.getContentType()
                ));
    }
}