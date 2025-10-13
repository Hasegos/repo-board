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
 * Caffeine 기반 캐시 설정 클래스.
 *
 * <p>{@link EnableCaching}을 통해 Spring의 애노테이션 캐싱 기능
 * ({@code @Cacheable}, {@code @CacheEvict}, {@code @CachePut})을 활성화한다.</p>
 *
 * <h3>등록 캐시 목록</h3>
 * <ul>
 *   <li><b>ghUser</b> — 사용자 정보 (1시간)</li>
 *   <li><b>ghRepos</b> — 사용자 레포지토리 목록 (5분)</li>
 *   <li><b>ghRepoById</b> — 단일 레포지토리 (5분)</li>
 *   <li><b>ghRepoReadmeById</b> — 레포지토리 README (5분)</li>
 *   <li><b>ghSearch</b> — 검색 결과 캐시 (10분)</li>
 *   <li><b>ghRefresh</b> — 전략 기반 강제 새로고침 결과 (10분)</li>
 *   <li><b>ghQuerySearch</b> — 전략별 쿼리 기반 결과 (10분)</li>
 * </ul>
 *
 * <p>모든 캐시는 최대 5,000개 항목과 TTL 기반 {@code expireAfterWrite} 정책을 사용하며,
 * {@code recordStats()}로 히트/미스 통계 수집도 가능하다.</p>
 *
 * <h3>주의</h3>
 * Caffeine은 JVM 메모리 기반이므로 캐시 크기 및 TTL 설정은 신중히 해야 함.
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
     * @return 등록된 캐시들을 관리하는 {@link CacheManager}
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
                        .maximumSize(5000)
                        .expireAfterWrite(Duration.ofMinutes(5))
                        .recordStats()
                        .build()
        );
        var repoByIdCache = new CaffeineCache(
                "ghRepoById",
                Caffeine.newBuilder()
                        .maximumSize(5000)
                        .expireAfterWrite(Duration.ofMinutes(5))
                        .recordStats()
                        .build()
        );
        var repoByReadmeCache = new CaffeineCache(
                "ghRepoReadmeById",
                Caffeine.newBuilder()
                        .maximumSize(5000)
                        .expireAfterWrite(Duration.ofMinutes(5))
                        .recordStats()
                        .build()
        );
        var searchCache = new CaffeineCache(
                "ghSearch",
                Caffeine.newBuilder()
                        .maximumSize(5000)
                        .expireAfterWrite(Duration.ofMinutes(10))
                        .recordStats()
                        .build()
        );
        var refreshCache = new CaffeineCache(
                "ghRefresh",
                Caffeine.newBuilder()
                        .maximumSize(5000)
                        .expireAfterWrite(Duration.ofMinutes(10))
                        .recordStats()
                        .build()
        );
        var queryCache = new CaffeineCache(
                "ghQuerySearch",
                Caffeine.newBuilder()
                        .maximumSize(5000)
                        .expireAfterWrite(Duration.ofMinutes(10))
                        .recordStats()
                        .build()
        );
        var m = new SimpleCacheManager();
        m.setCaches(List.of(userCache, reposCache,repoByIdCache,
                repoByReadmeCache, searchCache, refreshCache, queryCache));
        return m;
    }
}