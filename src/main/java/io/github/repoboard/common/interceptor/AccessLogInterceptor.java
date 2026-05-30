package io.github.repoboard.common.interceptor;

import io.github.repoboard.common.config.WebMvcConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 사용자 접근 로그를 기록하는 인터셉터.
 *
 * <p>Cloudflare 프록시 환경에서 실제 사용자 IP를 추적하기 위해
 * {@code CF-Connecting-IP} → {@code X-Forwarded-For} → {@code RemoteAddr}
 * 순으로 확인한다.</p>
 *
 * <p>로그 기록은 {@code afterCompletion} 시점에 응답 상태 코드까지 포함하여 남긴다.
 * 정적 리소스/파비콘 등은 {@link WebMvcConfig}에서 제외 경로로 등록한다.</p>
 */
@Component
public class AccessLogInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger("ACCESS");
    private static final int MAX_UA_LENGTH = 200;

    /**
     * 요청 완료 후 접근 로그를 기록한다.
     *
     * <p>로그 기록 실패 시에도 응답에는 영향을 주지 않는다.</p>
     *
     * @param request  HTTP 요청
     * @param response HTTP 응답
     * @param handler  핸들러
     * @param ex       처리 중 발생한 예외(없으면 null)
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        try {
            log.info("IP={} METHOD={} URI={} STATUS={} UA={}",
                    resolveClientIp(request),
                    request.getMethod(),
                    buildUri(request),
                    response.getStatus(),
                    sanitizeUa(request.getHeader("User-Agent")));
        } catch (Exception e) {
            log.warn("접근 로그 기록 실패: {}", e.getMessage());
        }
    }

    /**
     * 프록시 헤더 우선순위로 클라이언트 IP를 결정한다.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("CF-Connecting-IP");
        if (isValid(ip)) return ip;

        ip = request.getHeader("X-Forwarded-For");
        if (isValid(ip)) {
            int comma = ip.indexOf(',');
            return (comma > 0 ? ip.substring(0, comma) : ip).trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (isValid(ip)) return ip;

        String remote = request.getRemoteAddr();
        return (remote != null && !remote.isBlank()) ? remote : "unknown";
    }

    /**
     * URI에 query string이 있으면 합쳐서 반환한다.
     */
    private String buildUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String qs = request.getQueryString();
        return (qs != null) ? uri + "?" + qs : uri;
    }

    /**
     * User-Agent를 로그 안전하게 가공한다.
     * <ul>
     *   <li>CRLF/탭 제거 (로그 인젝션 방어)</li>
     *   <li>최대 {@value #MAX_UA_LENGTH}자로 제한</li>
     * </ul>
     */
    private String sanitizeUa(String ua) {
        if (ua == null || ua.isBlank()) {
            return "unknown";
        }
        String cleaned = ua.replaceAll("[\r\n\t]", "_");
        return cleaned.substring(0, Math.min(cleaned.length(), MAX_UA_LENGTH));
    }

    private boolean isValid(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
    }
}