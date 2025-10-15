package io.github.repoboard.common.domain;

import io.github.repoboard.security.core.CustomUserPrincipal;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 현재 로그인한 사용자의 username을 Auditing 시스템에 주입
 */
@Component("auditorAware")
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth ==  null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")){
            return Optional.of("SYSTEM");
        }
        Object principal = auth.getPrincipal();
        if(principal instanceof CustomUserPrincipal user){
            return Optional.of(user.getUsername());
        }

        return Optional.ofNullable(auth.getName());
    }
}