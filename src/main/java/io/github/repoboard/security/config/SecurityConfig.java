package io.github.repoboard.security.config;

import io.github.repoboard.security.oauth2.CustomOAuth2UserService;
import io.github.repoboard.security.userdetails.CustomUserDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 필터 체인, 인가/인증 정책, OAuth2 클라이언트 설정 등을 구성합니다.
 *
 * <p><strong>핵심</strong>
 * <ul>
 *   <li>불변/상태: 스레드 안전성, 빈 주기, 트랜잭션 경계 등을 고려합니다.</li>
 *   <li>예외: 인증/인가 실패, 외부 API 오류 등 발생 가능한 예외를 문서화합니다.</li>
 *   <li>보안: 민감 정보(토큰, 비밀번호) 로그 노출 금지 원칙을 따릅니다.</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableMethodSecurity
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomUserDetailService customUserDetailService;

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

    /**
     * DAO 기반 인증 Provider 빈을 등록합니다.
     * UserDetailsService 및 PasswordEncoder를 연결하여 폼 로그인 인증을 처리합니다.
     *
     * @return DaoAuthenticationProvider
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(customUserDetailService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    /**
     * Spring SecurityFilterChain을 구성합니다.
     * CSRF/헤더, 요청 인가 정책, 세션/폼로그인, OAuth2 로그인 성공/실패 핸들러 등을 설정합니다.
     *
     * @param http HttpSecurity 빌더
     * @return 빌드된 SecurityFilterChain
     * @throws Exception 빌드 중 예외
     */
    @Bean
    public SecurityFilterChain filterChain (HttpSecurity http) throws Exception{
        http
                .headers(header -> header.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/script/**").permitAll()
                        .requestMatchers("/error/**").permitAll()
                        .requestMatchers("/", "/users/login", "/users/signup","/oauth2/**","/login/**").permitAll()
                        .requestMatchers("/profile", "/repos").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/users/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/" , true)
                        .failureHandler((request, response, exception) -> {
                            String msg;

                            if(exception instanceof org.springframework.security.authentication.LockedException){
                                msg = "정지된 계정입니다.";
                            }else if(exception instanceof org.springframework.security.authentication.DisabledException){
                                msg = "탈퇴(비활성화된) 계정입니다.";
                            } else{
                                msg = "로그인에 실패했습니다. 잠시 후에 다시 시도해주세요.";
                            }
                            request.getSession().setAttribute("loginError",msg);
                            response.sendRedirect("/users/login");
                        })
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .deleteCookies("JSESSIONID")
                )
                .oauth2Login(oAuth2 -> oAuth2
                        .loginPage("/users/login")
                        .userInfoEndpoint(userInfo ->{
                                userInfo.userService(customOAuth2UserService);
                        })
                        .defaultSuccessUrl("/", true)
                        .failureHandler((request, response, exception) -> {
                            String msg = "소셜 로그인에 실패했습니다. 잠시 후 다시 시도해주세요.";

                            if (exception instanceof org.springframework.security.oauth2.core.OAuth2AuthenticationException oae) {
                                var err = oae.getError();
                                String code = (err != null) ? err.getErrorCode() : null;
                                String desc = (err != null && err.getDescription() != null) ? err.getDescription() : null;

                                if ("ACCOUNT_SUSPENDED".equals(code))      msg = (desc != null) ? desc : "정지된 계정입니다.";
                                else if ("ACCOUNT_DELETED".equals(code))   msg = (desc != null) ? desc : "탈퇴(비활성)된 계정입니다.";
                            }
                            request.getSession().setAttribute("loginError", msg);
                            response.sendRedirect("/users/login");
                        })
                );
        return http.build();
    }
}