package io.github.repoboard.service;

import io.github.repoboard.dto.GithubRepoDTO;
import io.github.repoboard.dto.GithubUserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub Public API(v3)를 사용하여 사용자 프로필과 레포지토리 목록을 조회하는 서비스 클래스.
 * <p>
 * 인증 토큰 없이 호출하므로, 공개(Public) 레포지토리만 조회 가능하며
 * API 호출 한도는 IP 기준 60 요청/시간으로 제한됩니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class GitHubApiService {

    private final WebClient githubWebClient;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /**
     * GitHub 프로필 URL에서 username을 추출하기 위한 정규 표현식.
     * 예: {@code https://github.com/Hasegos} → {@code Hasegos}
     */
    private static final Pattern USERNAME_IN_URL = Pattern.compile(
            "^https?://github\\.com/([A-Za-z0-9-]+)/*$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 입력값이 GitHub 프로필 URL이든 username이든 안전하게 username을 추출합니다.
     *
     * @param input GitHub URL 또는 username
     * @return 추출된 GitHub username
     * @throws IllegalArgumentException 입력이 비어있거나 올바른 형식이 아닐 경우
     */
    public String extractUsername(String input){

        if(input == null || input.trim().isEmpty()){
            throw new IllegalArgumentException("GitHub URL이 없습니다.");
        }

        String trimmed =  input.trim();

        /* username 만 있는 경우 */
        if(!trimmed.startsWith("http")){
            return trimmed.replaceAll("[^A-Za-z0-9-]", "");
        }

        /* URL로 입력 받은 경우 */
        Matcher m = USERNAME_IN_URL.matcher(trimmed);
        if(m.matches()){
            return m.group(1);
        }

        throw new IllegalArgumentException("올바른 GitHub URL이 아닙니다. 입력받은 url : " + input);
    }

    /**
     * GitHub 사용자 프로필 정보 조회
     * <p>
     * 인증 토큰 없이 호출되므로, 비공개 프로필 항목은 조회되지 않습니다.
     * </p>
     *
     * @param username GitHub 사용자명
     * @return {@link GithubUserDTO} 객체
     * @throws RuntimeException 사용자 정보 조회 실패 시
     */
    @Cacheable(value = "ghUser", key = "#username", sync = true)
    public GithubUserDTO getUser(String username){
        try{
            return githubWebClient.get()
                    .uri("/users/{username}", username)
                    .retrieve()
                    .bodyToMono(GithubUserDTO.class)
                    .timeout(TIMEOUT)
                    .block();
        }catch (WebClientResponseException.NotFound e){
            throw new RuntimeException("사용자를 찾을 수 없습니다 : " + username);
        }catch (Exception e){
            throw new RuntimeException("사용자 정보 조회 중 오류 발생", e);
        }
    }

    /**
     * 특정 GitHub 사용자가 소유한 공개 레포지토리 목록을 페이징하여 모두 조회합니다.    *
     * <p>
     * GitHub API의 {@code /users/{username}/repos} 엔드포인트를 호출하며,
     * 인증 토큰 없이 호출하므로 비공개(private) 레포지토리는 포함되지 않습니다.
     * </p>
     *
     * 소유 레포 전체(공개) 조회 + 필요 시 fork 제외 (캐시)
     * - 내부에서 GitHub 서버사이드 페이징(Link 헤더) 순회
     * - 캐시 TTL(5분) 내에서는 재호출 없이 반환
     *
     * @param username    GitHub 사용자명
     * @param includeForks {@code true}면 fork 레포지토리 포함, {@code false}면 제외
     * @return {@link GithubRepoDTO} 리스트 (공개 레포만)
     * @throws RuntimeException API 호출 실패 시
     */
    @Cacheable(value = "ghRepos", key = "#username + ':' + #includeForks", sync = true)
    public List<GithubRepoDTO> getOwnedRepos(String username, boolean includeForks){

        List<GithubRepoDTO> all = new ArrayList<>();
        int page = 1;

        while (true){
            final int finalPage = page;
            ResponseEntity<List<GithubRepoDTO>> entity =
                    githubWebClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/users/{username}/repos")
                                    .queryParam("type", "owner")
                                    .queryParam("sort", "updated")
                                    .queryParam("per_page","100")
                                    .queryParam("page", finalPage)
                                    .build(username))
                            .retrieve()
                            .toEntityList(GithubRepoDTO.class)
                            .timeout(TIMEOUT)
                            .block();

            List<GithubRepoDTO> pageData = Objects.requireNonNull(entity).getBody();
            if(pageData == null || pageData.isEmpty()) break;

            if(!includeForks){
                pageData.removeIf(repo -> Boolean.TRUE.equals(repo.getFork()));
            }
            all.addAll(pageData);

            String link = entity.getHeaders().getFirst(HttpHeaders.LINK);
            if(link == null || !link.contains("rel=\"next\"")) break;
            page++;
        }
        return all;
    }
}