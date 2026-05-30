package io.github.repoboard.common.config;

import io.github.repoboard.common.interceptor.AccessLogInterceptor;
import io.github.repoboard.common.interceptor.ClientIpInterceptor;
import io.github.repoboard.common.interceptor.HostValidationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 인터셉터 등록 설정.
 *
 * <p><strong>실행 순서</strong>
 * <ol>
 *   <li>{@link HostValidationInterceptor} — Host 헤더 검증 (차단 우선)</li>
 *   <li>{@link ClientIpInterceptor} — MDC clientIp 주입</li>
 *   <li>{@link AccessLogInterceptor} — 접근 로그 기록 (응답 후)</li>
 * </ol>
 * </p>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ClientIpInterceptor clientIpInterceptor;
    private final HostValidationInterceptor hostValidationInterceptor;
    private final AccessLogInterceptor accessLogInterceptor;

    private static final String[] EXCLUDED = {
            "/css/**", "/js/**", "/images/**", "/script/**",
            "/favicon.ico", "/robots.txt"
    };

    /**
     * 인터셉터 체인 등록.
     *
     * @param registry 인터셉터 등록 레지스트리
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(hostValidationInterceptor)
                .order(0)
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDED);

        registry.addInterceptor(clientIpInterceptor)
                .order(1)
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDED);

        registry.addInterceptor(accessLogInterceptor)
                .order(2)
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDED);
    }
}