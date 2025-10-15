package io.github.repoboard.security.oauth2;

import io.github.repoboard.dto.github.GithubUserDTO;
import io.github.repoboard.model.User;
import io.github.repoboard.model.enums.UserProvider;
import io.github.repoboard.model.enums.UserRoleType;
import io.github.repoboard.model.enums.UserStatus;
import io.github.repoboard.repository.DeleteUserRepository;
import io.github.repoboard.repository.UserRepository;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.GitHubApiService;
import io.github.repoboard.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * <p>OAuth2 로그인 시 사용자 정보를 가져오고 애플리케이션 User로 매핑하는 서비스입니다.</p><br>
 *
 * 구글/깃허브 등 Provider 응답을 표준화하고,<br>
 * 신규 사용자의 경우 계정을 생성하며,<br>
 * 필요 시 임시 비밀번호/고유 username을 생성합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileService profileService;
    private final GitHubApiService gitHubApiService;
    private final DeleteUserRepository deleteUserRepository;

    @Qualifier("githubWebClient")
    private final WebClient githubWebClient;

    /**
     * OAuth2 인증 이후 사용자 정보를 조회하여 애플리케이션 계정과 매핑합니다.
     *
     * @param userRequest 클라이언트 등록 정보와 AccessToken이 포함된 요청
     * @return OAuth2User (CustomUserPrincipal 등)
     * @throws OAuth2AuthenticationException Provider 측 오류나 매핑 실패
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try{
            return socialLogin(userRequest, oAuth2User);
        }catch (OAuth2AuthenticationException ex){
            log.error(ex.getMessage());
            throw ex;
        }
    }

    /**
     * OAuth2 로그인 사용자 정보를 표준화하고 앱의 User 도메인에 매핑한다.
     *
     * <p>기존 providerId가 있으면 로그인 처리, 없으면 신규 사용자 생성.</p>
     * <p>7일 이내 탈퇴한 동일 providerId가 존재하면 예외 발생.</p>
     * <p>폼 가입된 이메일과 중복되면 예외 발생.</p>
     *
     * <p><b>Google</b>: id=sub, email=email</p>
     * <p><b>GitHub</b>: id=id, email 없으면 /user/emails 로 보완</p>
     *
     * @param req OAuth2 요청 정보
     * @param ou  Provider에서 받은 사용자 정보
     * @return 매핑된 CustomUserPrincipal
     * @throws OAuth2AuthenticationException 정책 위반 또는 필수 정보 누락 시
     */
    private OAuth2User socialLogin(OAuth2UserRequest req, OAuth2User ou){

        String regId = req.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = ou.getAttributes();

        String socialId;
        String email;
        UserProvider provider;

        switch (regId){
            case "google" -> {
                socialId = (String) attributes.get("sub");
                email = (String) attributes.get("email");
                provider = UserProvider.GOOGLE;

                if(socialId == null || socialId.isBlank()){
                    throw new OAuth2AuthenticationException("구글 계정 ID(sub)를 가져오지 못했습니다.");
                }
                if(email == null || email.isBlank()){
                    throw new OAuth2AuthenticationException("구글 계정 이메일 권한이 없습니다.");
                }
            }
            case "github" -> {
                Object idObj = attributes.get("id");
                socialId = (idObj == null) ? null : String.valueOf(idObj);
                provider = UserProvider.GITHUB;

                email = (String) attributes.get("email");
                if(socialId == null || socialId.isBlank()){
                    throw new OAuth2AuthenticationException("GitHub 계정 ID(sub)를 가져오지 못했습니다.");
                }
                if(email == null || email.isBlank()){
                    email = fetchGithubPrimaryEmail(req.getAccessToken().getTokenValue());
                    if(email == null || email.isBlank()){
                        throw new OAuth2AuthenticationException("구글 계정 이메일 권한이 없습니다.");
                    }
                }
            }
            default -> throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 Provider: " + regId);
        }

        Optional<User> userOptional = userRepository.findByProviderId(socialId);
        if(userOptional.isPresent()){
            User user = userOptional.get();

            if(user.getStatus() == UserStatus.SUSPENDED){
                throw new OAuth2AuthenticationException(new OAuth2Error("SUSPENDED"));
            }

            if(user.getStatus() == UserStatus.DELETED){
                throw new OAuth2AuthenticationException(new OAuth2Error("DELETED"));
            }

            return new CustomUserPrincipal(user, attributes);
        }

        deleteUserRepository.findByProviderId(socialId)
                .ifPresent(backup -> {
                    Instant sevenDaysAgo = Instant.now().minus(Duration.ofDays(7));

                    if(backup.getStatus() == UserStatus.DELETED &&
                       backup.getDeleteAt().isAfter(sevenDaysAgo)){
                        throw new OAuth2AuthenticationException(new OAuth2Error("DELETED"));
                    }
                });

        userRepository.findByUsername(email).ifPresent(u -> {
            if(u.getProviderId() == null || u.getProviderId().isBlank()){
                throw new OAuth2AuthenticationException("이미 폼 로그인으로 가입된 이메일입니다. 아이디/비밀번호로 로그인해주세요.");
            }
        });

        String randomPassword = generateSecureRandomPassword();

        User user = new User();
        user.setUsername(generateUniqueUsername(email));
        user.setPassword(passwordEncoder.encode(randomPassword));
        user.setRole(UserRoleType.ROLE_USER);
        user.setProviderId(socialId);
        user.setProvider(provider);

        try{
            userRepository.save(user);
        } catch (DataIntegrityViolationException e){
            user = userRepository.findByProviderId(socialId).orElseThrow(() -> e);
        }
        log.info("신규 OAuth2 사용자 생성 : {} (provider={})", email, regId);

        if(provider == UserProvider.GITHUB){
            String login = String.valueOf(attributes.get("login"));
            if(profileService.findProfileByUserId(user.getId()).isEmpty()){
                try{
                    GithubUserDTO dto = gitHubApiService.getUser(login);
                    profileService.registerProfile(user.getId(), dto);
                }catch (IOException e){
                    log.error("Github API 호출 실패");
                    throw new OAuth2AuthenticationException("Github 프로필 동기화 중 오류 발생");
                }
            }
        }
        return new CustomUserPrincipal(user,attributes);
    }

    /**
     * GitHub 사용자의 기본(primary) + 검증(verified) 이메일을 API로 조회한다.
     *
     * <p>우선순위</p>
     * <ol>
     *   <li>primary && verified</li>
     *   <li>verified 중 임의 1건</li>
     *   <li>없으면 {@code null}</li>
     * </ol>
     *
     * @param accessToken GitHub OAuth Access Token (Bearer)
     * @return 선택된 이메일 문자열, 없으면 {@code null}
     */
    private String fetchGithubPrimaryEmail(String accessToken){
        try {
            List<Map<String, Object>> emails = githubWebClient.get()
                    .uri("/user/emails")
                    .headers(h -> h.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    })
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (emails == null || emails.isEmpty()) return null;

            return emails.stream()
                    .filter(m -> Boolean.TRUE.equals(m.get("primary"))
                            && Boolean.TRUE.equals(m.get("verified")))
                    .map(m -> Objects.toString(m.get("email"), null))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseGet(() -> emails.stream()
                            .filter(m -> Boolean.TRUE.equals(m.get("verified")))
                            .map(m -> Objects.toString(m.get("email"), null))
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null));
        } catch (Exception e){
            log.warn("GitHub 이메일 조회 실패 : {}", e.getMessage());
            return null;
        }
    }

    /**
     * 소셜 전용 신규 계정에 부여할 임시 비밀번호를 생성한다.
     * @return 시간정보를 포함한 랜덤 문자열
     */
    private String generateSecureRandomPassword(){
        return UUID.randomUUID() + "-" + System.currentTimeMillis();
    }

    /**
     * 이메일의 로컬 파트(＠ 앞)를 기반으로 중복 없이 username을 만든다.
     * @param email 사용자 이메일 (반드시 ＠ 포함)
     * @return 중복 없는 username
     */
    private String generateUniqueUsername(String email){
        String base = email.substring(0, email.indexOf("@"));
        String username = base;
        int idx = 1;
        while (userRepository.existsByUsername(username)){
            username = base + idx++;
        }
        return username;
    }
}