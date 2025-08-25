package io.github.repoboard.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * GitHub API 호출을 위한 {@link WebClient} 설정 클래스.
 * <p>
 * 기본 URL, 타임아웃, User-Agent, Accept 헤더 등을 설정합니다.
 * </p>
 */
@Configuration
public class WebClientConfig {

    /**
     * GitHub API 호출용 WebClient Bean 생성
     *
     * @param baseUrl GitHub API 기본 URL
     * @return 설정된 {@link WebClient} 인스턴스
     */
    @Bean
    public WebClient githubWebClient(
            @Value("${github.api.base-url:https://api.github.com}") String baseUrl
    ){
        HttpClient httpClient =  HttpClient.create()
                .responseTimeout(Duration.ofSeconds(10));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("User-Agent", "RepoBoard-App")
                .exchangeStrategies(
                        ExchangeStrategies.builder()
                                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                                .build()
                )
                .build();
    }
}