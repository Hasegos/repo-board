package io.github.repoboard.security.config;

import io.github.repoboard.common.exception.GithubRateLimitException;
import io.github.repoboard.common.exception.UnexpectedContentTypeException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GitHub API 호출을 위한 {@link WebClient} 설정 클래스.
 * <p>
 * 이 클래스는 GitHub API 호출 시 필요한 기본 설정을 제공하며, <br>
 * 토큰 로테이션, Rate Limit 처리, Content-Type 검증 등의 로직을 포함한다.
 * </p>
 */
@Configuration
public class WebClientConfig {

    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    @Value("${github.api.base-url}")
    private String baseUrl;

    @Value("${github.api.token}")
    private String rawTokens;

    private List<String> tokens;
    private final AtomicInteger tokenIndex = new AtomicInteger(0);
    private final Map<String, Long> tokenLastUserMap = new ConcurrentHashMap<>();
    private static final Duration REQUEST_INTERVAL = Duration.ofSeconds(1);

    /**
     * Bean 초기화 시점에 토큰 문자열을 파싱하여 List로 변환
     */
    @PostConstruct
    public void init() {
        if (rawTokens == null || rawTokens.isBlank()) {
            throw new IllegalArgumentException("Github API 토큰이 하나도 설정되지 않았습니다.");
        }
        tokens = Arrays.stream(rawTokens.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * 순차적으로 다음 GitHub API 토큰을 가져온다.
     * <p>토큰별 요청 간격을 최소 {@link #REQUEST_INTERVAL} 만큼 유지한다.</p>
     *
     * @return 사용할 GitHub API 토큰
     */
    private String getNextToken(){
        if(tokens == null || tokens.isEmpty()){
            throw new IllegalArgumentException("Github API 토큰이 하나도 설정되지 않았습니다.");
        }
        String token = tokens.get(tokenIndex.getAndUpdate(i -> (i + 1) % tokens.size()));
        long now = System.currentTimeMillis();
        long lastUsed =  tokenLastUserMap.getOrDefault(token, 0L);
        long wait = lastUsed + REQUEST_INTERVAL.toMillis() - now;

        if(wait > 0){
            try {
                Thread.sleep(wait);
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }
        tokenLastUserMap.put(token, System.currentTimeMillis());
        return token;
    }

    /**
     * GitHub API 호출용 {@link WebClient} Bean 생성
     *
     * @return 설정된 {@link WebClient} 인스턴스
     */
    @Bean
    public WebClient githubWebClient(){
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .responseTimeout(Duration.ofSeconds(10))))
                .exchangeStrategies(defaultExchangeStrategies())
                .defaultHeaders(this::applyDefaultHeaders)
                .filter(this::githubAuthFilter)
                .build();
    }

    /**
     * WebClient의 메모리 버퍼 및 로깅 설정
     *
     * @return {@link ExchangeStrategies} 설정 객체
     */
    private ExchangeStrategies defaultExchangeStrategies() {
        return ExchangeStrategies.builder()
                .codecs(cfg -> {
                    cfg.defaultCodecs().maxInMemorySize(4 * 1024 * 1024);
                    cfg.defaultCodecs().enableLoggingRequestDetails(true);
                })
                .build();
    }

    /**
     * 모든 요청에 기본적으로 적용할 헤더 설정
     * <p>
     * - User-Agent: RepoBoard/1.0 <br>
     * - Accept: application/vnd.github.v3+json
     * </p>
     *
     * @param headers 요청 헤더 객체
     */
    private void applyDefaultHeaders(HttpHeaders headers){
        headers.set(HttpHeaders.USER_AGENT, "RepoBoard/1.0");
        headers.set(HttpHeaders.ACCEPT, "application/vnd.github.v3+json");
    }

    /**
     * GitHub API 요청 전 필터
     * <p>
     * - Authorization 헤더가 없는 경우 토큰을 자동 설정 <br>
     * - 403/429 상태 코드 시 {@link GithubRateLimitException} 발생 <br>
     * - Content-Type 검증 실패 시 {@link UnexpectedContentTypeException} 발생
     * </p>
     *
     * @param request 원본 요청
     * @param next    다음 필터 또는 요청 실행
     * @return {@link Mono} 형태의 응답
     */
    private Mono<ClientResponse> githubAuthFilter(ClientRequest request, ExchangeFunction next) {
        boolean hasAuthHeader = request.headers().containsKey(HttpHeaders.AUTHORIZATION);
        ClientRequest filtered = ClientRequest.from(request)
                .headers(h -> {
                    if (!hasAuthHeader) {
                        String token = getNextToken();
                        h.setBearerAuth(token);
                        log.debug("🔻 WebClientConfig에서 설정한 토큰: {} " , token);
                    } else {
                        log.debug("✅ Authorization 헤더가 이미 존재 → 토큰 덮어쓰기 생략");
                    }
                })
                .build();
        return next.exchange(filtered)
                .flatMap(response -> {
                    int status = response.statusCode().value();
                    String contentType = response.headers().contentType()
                            .map(MediaType::toString)
                            .orElse("unknown");
                    if(status == 403 || status == 429){
                        String reset = response.headers().header("X-RateLimit-Reset")
                                .stream().findFirst().orElse("0");
                        long restTime = parseEpoch(reset);
                        log.warn("🔒 Rate Limited - Reset: {} (epoch)", reset);
                        return Mono.error(new GithubRateLimitException("Rate Limited", restTime));
                    }
                    MediaType mediaType =  MediaType.parseMediaType(contentType);
                    if (!MediaType.APPLICATION_JSON.includes(mediaType) && !contentType.contains("vnd.github")) {
                        log.warn("⚠️ 예상하지 못한 Content-Type: {}", contentType);
                        // 빈 응답으로 처리하거나 에러로 처리
                        return Mono.error(new UnexpectedContentTypeException(contentType));
                    }
                    return Mono.just(response);
                })
                .retryWhen(Retry.fixedDelay(3,Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(10))
                        .jitter(0.5) // 지연시간 랜덤성 추가
                        .filter(err -> err instanceof GithubRateLimitException)
                        .doBeforeRetry(retrySignal ->
                                log.info("🔄 재시도  {} 회차", (retrySignal.totalRetries() + 1))
                        )
                );
    }

    /**
     * 문자열 형태의 epoch 값을 long 형태로 변환
     * <p>
     * 변환 실패 시 현재 시각 + 60초를 기본값으로 반환
     * </p>
     *
     * @param raw 문자열 epoch 값
     * @return 변환된 epoch 시간
     */
    private long parseEpoch(String raw){
        try{
            return Long.parseLong(raw);
        }catch (NumberFormatException e){
            return Instant.now().getEpochSecond() + 60;
        }
    }
}