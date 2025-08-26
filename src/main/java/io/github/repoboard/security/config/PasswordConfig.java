package io.github.repoboard.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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