package io.github.repoboard.security.config;

import io.github.repoboard.security.oauth2.CustomOAuth2UserService;
import io.github.repoboard.security.userdetails.CustomUserDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

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
    private final CustomAuthFailureHandler customAuthFailureHandler;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.csp:}")
    private String cspDirectives;

    @Value("${app.security.cspReportOnly:false}")
    private boolean cspReportOnly;

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
        p.setPasswordEncoder(passwordEncoder);
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
            .headers(header ->
                header.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                .referrerPolicy(ref -> ref.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .contentTypeOptions(Customizer.withDefaults())
                .contentSecurityPolicy(c -> {
                    if (cspDirectives != null && !cspDirectives.isBlank()){
                        String sanitized = cspDirectives.replaceAll("[\r\n]+", " ");
                        c.policyDirectives(sanitized);
                        if(cspReportOnly) c.reportOnly();
                    }
                })
            )
            .csrf(csrf -> csrf
                    .ignoringRequestMatchers( "/api/**"))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/css/**", "/js/**", "/images/**", "/script/**").permitAll()
                    .requestMatchers("/error/**").permitAll()
                    .requestMatchers("/", "/users/login", "/users/signup","/oauth2/**","/login/**").permitAll()
                    .requestMatchers("/api/repos","/search/**").permitAll()
                    .requestMatchers("/admin/**", "/actuator/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                    .accessDeniedHandler((req, res , e)
                            -> res.sendRedirect("/"))
            )
            .formLogin(form -> form
                    .loginPage("/users/login")
                    .loginProcessingUrl("/login")
                    .defaultSuccessUrl("/" , true)
                    .failureHandler(customAuthFailureHandler)
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
                    .failureHandler(customAuthFailureHandler)
            );
        return http.build();
    }
}