package io.github.repoboard.security.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Caffeine 기반 스프링 캐시 설정 클래스.
 *
 * <p>{@link EnableCaching}으로 애노테이션 기반 캐싱({@code @Cacheable}, {@code @CacheEvict}, {@code @CachePut})을 활성화한다.
 * 본 설정은 다음 두 캐시를 등록한다:
 * <ul>
 *   <li><b>ghUser</b> — 최대 1,000 엔트리, 쓰기 기준 1시간 만료</li>
 *   <li><b>ghRepos</b> — 최대 1,000 엔트리, 쓰기 기준 5분 만료</li>
 * </ul>
 *
 * <p>{@code recordStats()}를 활성화하여 히트/미스 통계를 수집한다.<br>
 * (Actuator 캐시 엔드포인트 등에서 확인 가능).
 *
 * <h3>스레드 안전성</h3>
 * Caffeine은 동시성에 최적화된 내부 자료구조를 사용하므로 멀티스레드 환경에서도 안전하게 동작한다.
 *
 * <h3>사용 예</h3>
 * <pre>{@code
 * @Service
 * public class GithubService {
 *   @Cacheable(cacheNames = "ghUser", key = "#login")
 *   public GithubUser findUser(String login) { ... }
 *
 *   @Cacheable(cacheNames = "ghRepos", key = "#login")
 *   public List<Repo> findRepos(String login) { ... }
 * }
 * }</pre>
 *
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 애플리케이션 전역에서 사용할 {@link CacheManager} 빈을 생성한다.
     *
     * <p>{@link SimpleCacheManager}에 명시적으로 구성한 {@link CaffeineCache}들을 등록하며,
     * 각 캐시는 최대 용량과 {@code expireAfterWrite} 정책을 갖는다.
     *
     * @return "ghUser", "ghRepos" 캐시를 관리하는 {@code CacheManager}
     */
    @Bean
    public CacheManager cacheManager(){
        var userCache = new CaffeineCache(
                "ghUser",
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(Duration.ofHours(1))
                        .recordStats()
                        .build()

        );
        var reposCache = new CaffeineCache(
                "ghRepos",
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(Duration.ofMinutes(5))
                        .recordStats()
                        .build()
        );
        var m = new SimpleCacheManager();
        m.setCaches(List.of(userCache, reposCache));
        return m;
    }
}