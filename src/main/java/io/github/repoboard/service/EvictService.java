package io.github.repoboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ghRepos 캐시에서 특정 사용자(prefix=username:) 키만 선택적으로 무효화(Evict)한다.
 * <p>
 * - 사용처: 프로필 새로고침 직후 목록 불일치 방지.<br>
 * - 전제: ghRepos 캐시 키 포맷이 "username:page:size".<br>
 * - 구현: CaffeineCache 네이티브 키셋을 스캔해 접두사 일치 항목만 invalidate.<br>
 * - 폴백: CaffeineCache가 아니면 cache.clear() 수행.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvictService {

    private final CacheManager cacheManager;

    /**
     * ghRepos 캐시에서 {@code username + ":"}로 시작하는 키만 일괄 제거한다.
     *
     * @param username GitHub 사용자명 (null/blank 금지)
     */
    public void evictReposByUsername(String username){
        var cache = cacheManager.getCache("ghRepos");
        if(!(cache instanceof CaffeineCache cc)){
            if(cache != null) {
                cache.clear();
            }
            return;
        }
        var nativeCache = cc.getNativeCache();
        List<Object> targets = new ArrayList<>();
        for(Object k : nativeCache.asMap().keySet()){
            if(k != null && k.toString().startsWith(username + ":")){
                targets.add(k);
            }
        }
        nativeCache.invalidateAll(targets);
        log.info("[EVICT] ghRepos {}건 제거(prefix='{}:')", targets.size(), username);
    }
}