package io.github.repoboard.common.handler;

import io.github.repoboard.common.exception.GithubRateLimitException;
import io.github.repoboard.common.exception.UnexpectedContentTypeException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Map;

/**
 * 전역 예외 처리기(Global Exception Handler).
 *
 * <p>컨트롤러 계층에서 발생하는 예외를 한 곳에서 처리한다.</p>
 *
 * <p><strong>응답 분기</strong>
 * <ul>
 *   <li>{@code /api/**} 요청: JSON 응답</li>
 *   <li>그 외 (페이지) 요청: {@code error/error} 뷰 렌더링</li>
 * </ul>
 * </p>
 *
 * <p>커스텀 예외({@link GithubRateLimitException}, {@link UnexpectedContentTypeException})는
 * API 응답을 전제로 하므로 항상 JSON으로 반환한다.</p>
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String API_PREFIX = "/api/";

    /**
     * 요청 URI가 API 경로인지 판별한다.
     *
     * @param request HTTP 요청
     * @return API 요청 여부
     */
    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith(API_PREFIX);
    }

    /**
     * GitHub API Rate Limit 예외 처리
     *
     * @param ex GithubRateLimitException
     * @return HTTP 429 Too Many Requests + 제한 해제 시간 정보
     */
    @ExceptionHandler(GithubRateLimitException.class)
    public ResponseEntity<?> handleGithubRateLimit(GithubRateLimitException ex){
        long waitSeconds = ex.getResetTimeStamp() - Instant.now().getEpochSecond();
        log.warn("[GitHub Rate Limit] retryAfter={}s", waitSeconds);
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
        log.warn("[GitHub ContentType Mismatch] contentType={}", ex.getContentType());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "error", "Github API로부터 예상치 못한 Content-Type 응답",
                        "contentType", ex.getContentType()
                ));
    }

    /**
     * 존재하지 않는 경로 접근 시 404 처리.
     *
     * <p>브라우저 요청({@code Accept: text/html})은 에러 페이지로 렌더링하고,
     * favicon 등 자동 리소스 요청은 빈 응답으로 처리한다.</p>
     *
     * @param request HTTP 요청
     * @param ex      발생한 예외
     * @param model   뷰 모델
     * @return API 요청 시 JSON, 페이지 요청 시 에러 뷰
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNotFound(HttpServletRequest request, Exception ex, Model model){
        String uri = request.getRequestURI();
        log.info("[404] uri={}", uri);

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not Found", "path", uri));
        }

        String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (accept == null || !accept.contains(MediaType.TEXT_HTML_VALUE)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        model.addAttribute("status", 404);
        model.addAttribute("title", "페이지를 찾을 수 없습니다");
        model.addAttribute("message", "요청하신 주소가 없거나 이동되었습니다.");
        return "error/error";
    }

    /**
     * 처리되지 않은 모든 예외에 대한 500 처리.
     *
     * @param request HTTP 요청
     * @param ex      발생한 예외
     * @param model   뷰 모델
     * @return API 요청 시 JSON, 페이지 요청 시 에러 뷰
     */
    @ExceptionHandler(Exception.class)
    public Object handleUnexpected(HttpServletRequest request, Exception ex, Model model){
        log.error("[500] uri={}", request.getRequestURI(), ex);

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal Server Error"));
        }

        model.addAttribute("status", 500);
        model.addAttribute("title", "서버 오류가 발생했습니다");
        model.addAttribute("message", "잠시 후 다시 시도해 주세요.");
        return "error/error";
    }
}