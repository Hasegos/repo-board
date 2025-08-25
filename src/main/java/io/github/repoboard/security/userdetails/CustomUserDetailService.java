package io.github.repoboard.security.userdetails;

import io.github.repoboard.model.User;
import io.github.repoboard.repository.UserRepository;
import io.github.repoboard.security.core.CustomUserPrincipal;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 폼 로그인 시 username으로 사용자를 조회하는 UserDetailsService 구현체입니다. <br><br>
 * UserRepository를 통해 사용자 정보를 조회하고, CustomUserPrincipal로 감싸 반환합니다.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * username으로 사용자를 조회합니다.
     *
     * @param username 사용자 식별자
     * @return UserDetails 구현체 (CustomUserPrincipal)
     * @throws UsernameNotFoundException 사용자가 없을 경우
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));
        return new CustomUserPrincipal(user);
    }
}