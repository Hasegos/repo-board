package io.github.repoboard.security.core;

import io.github.repoboard.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * <p>Spring Security의 UserDetails 및 OAuth2User를 동시에 구현한 사용자 Principal 구현체입니다.</p><br>
 * 도메인 User 엔티티를 감싸며, 권한/이름/속성 제공을 표준화합니다.
 */
public class CustomUserPrincipal implements OAuth2User, UserDetails {

    private final User user;
    private final Map<String, Object> attributes;

    public CustomUserPrincipal(User user){
        this.user = user;
        this.attributes = Collections.emptyMap();
    }

    public CustomUserPrincipal(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    public User getUser(){return user;}

    /* ===== OAuth2 ===== */
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return attributes.getOrDefault("sub", user.getProviderId()).toString();
    }

    /* ===== UserDetails ===== */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    @Override
    public String getPassword() { return user.getPassword(); }

    @Override
    public String getUsername() { return user.getUsername(); }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}