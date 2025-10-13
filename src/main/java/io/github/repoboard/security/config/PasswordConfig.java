package io.github.repoboard.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 암호화를 위한 보안 설정 클래스.
 *
 * <p>BCrypt 기반의 {@link PasswordEncoder} 빈을 등록하여
 * 사용자 비밀번호를 안전하게 해싱하고 검증한다.</p>
 */
@Configuration
public class PasswordConfig {

    /**
     * PasswordEncoder 빈을 등록합니다.
     * BCrypt 해시를 사용하여 비밀번호를 안전하게 저장/검증합니다.
     *
     * @return BCrypt 기반 PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}