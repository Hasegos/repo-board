package io.github.repoboard.security.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GitHub API 호출을 위한 {@link WebClient} 설정 클래스.
 * <p>
 * 기본 URL, 타임아웃, User-Agent, Accept 헤더 등을 설정합니다.
 * </p>
 */
@Configuration
public class WebClientConfig {

    @Value("${github.api.base-url}")
    private String baseUrl;

    @Value("${github.api.token}")
    private String rawTokens;

    private List<String> tokens;
    private final AtomicInteger tokenIndex = new AtomicInteger(0);
    private final Object rateLimitLock = new Object();
    private final Map<String, Long> tokenLastUserMap = new ConcurrentHashMap<>();
    private static final Duration REQUEST_INTERVAL = Duration.ofSeconds(1);

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
     * GitHub API 호출용 WebClient Bean 생성
     *
     * @return 설정된 {@link WebClient} 인스턴스
     */
    @Bean
    public WebClient githubWebClient(){
        HttpClient httpClient =  HttpClient.create()
                .responseTimeout(Duration.ofSeconds(10));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(
                        ExchangeStrategies.builder()
                                .codecs(cfg ->{
                                    cfg.defaultCodecs().maxInMemorySize(4 * 1024 * 1024);
                                    cfg.defaultCodecs().enableLoggingRequestDetails(true);
                                })
                                .build()
                )
                .defaultHeaders(headers -> {
                    headers.set(HttpHeaders.USER_AGENT, "RepoBoard/1.0");
                    headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                })
                .filter((request, next) -> {
                    String token = getNextToken();
                    ClientRequest filtered = ClientRequest.from(request)
                            .headers(h -> {
                                h.set(HttpHeaders.USER_AGENT, "PostmanRuntime/7.45.0");
                                h.set(HttpHeaders.ACCEPT, "application/vnd.github.v3+json");
                                h.setBearerAuth(token);
                                System.out.println("🔻 요청 헤더 설정됨: " + h);
                            })
                            .build();
                    return next.exchange(filtered)
                            .flatMap(response -> {
                                int status = response.statusCode().value();
                                String contentType = response.headers().contentType()
                                        .map(MediaType::toString)
                                        .orElse("unknown");
                                if(status == 403 || status == 429){
                                    String rateLimitRemaining = response.headers().header("X-RateLimit-Remaining")
                                            .stream().findFirst().orElse("unknown");
                                    String rateLimitReset = response.headers().header("X-RateLimit-Reset")
                                            .stream().findFirst().orElse("unknown");

                                    System.out.println("🔒 Rate Limited - Remaining: " + rateLimitRemaining +
                                            ", Reset: " + rateLimitReset);
                                    return Mono.error(new RuntimeException("GitHub Rate Limited"));
                                }
                                if (!contentType.contains("json") && !contentType.contains("application/vnd.github")) {
                                    System.out.println("⚠️ 예상하지 못한 Content-Type: " + contentType);
                                    // 빈 응답으로 처리하거나 에러로 처리
                                    return Mono.error(new RuntimeException("Unexpected Content-Type: " + contentType));
                                }
                                return Mono.just(response);
                            })
                            .retryWhen(Retry.fixedDelay(3,Duration.ofSeconds(2))
                                    .maxBackoff(Duration.ofSeconds(10))
                                    .filter(err -> err instanceof RuntimeException)
                                    .doBeforeRetry(retrySignal ->
                                            System.out.println("🔄 재시도 " + (retrySignal.totalRetries() + 1) + "회차")
                                    )
                            );
                })
                .build();
    }
}