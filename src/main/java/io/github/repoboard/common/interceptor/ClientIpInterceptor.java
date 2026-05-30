package io.github.repoboard.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 요청 클라이언트 IP를 MDC에 주입하는 인터셉터.
 *
 * <p>로그 패턴의 {@code [%X{clientIp}]} 에 실제 IP가 표시되도록 한다.<br>
 * nginx + Cloudflare 환경을 고려하여 우선순위에 따라 IP를 추출한다.</p>
 *
 * <p><strong>IP 추출 우선순위</strong>
 * <ol>
 *   <li>{@code CF-Connecting-IP} (Cloudflare 원본 IP)</li>
 *   <li>{@code X-Forwarded-For} (nginx 등 프록시 체인의 첫번째)</li>
 *   <li>{@code X-Real-IP} (nginx 표준)</li>
 *   <li>{@code request.getRemoteAddr()} (최종 fallback)</li>
 * </ol>
 * </p>
 */
@Component
public class ClientIpInterceptor implements HandlerInterceptor {

    private static final String MDC_KEY = "clientIp";
    private static final String UNKNOWN = "unknown";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler){
        MDC.put(MDC_KEY, resolveClientIp(request));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex){
        MDC.remove(MDC_KEY);
    }

    /**
     * 프록시 헤더 우선순위에 따라 클라이언트 IP를 결정한다.
     *
     * @param request HTTP 요청
     * @return 클라이언트 IP, 식별 불가 시 "unknown"
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
        return (remote != null && !remote.isBlank()) ? remote : UNKNOWN;
    }

    /**
     * 헤더 값의 유효성을 검사한다.
     */
    private boolean isValid(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
    }
}