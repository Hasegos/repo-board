package io.github.repoboard.service;

import io.github.repoboard.dto.GithubRepoDTO;
import io.github.repoboard.dto.GithubSearchResponse;
import io.github.repoboard.dto.GithubUserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
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
     * GitHub URL에서 username 자체 검증 (1~39자, 앞/뒤 하이픈 금지)
     * 예 : {@code Hasegos}
     */
    private static final Pattern GH_USER = Pattern.compile(
        "^[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?$"
    );

    /**
     * GitHub 프로필 URL에서 username을 추출하기 위한 정규 표현식.
     * 예: {@code https://github.com/Hasegos} → {@code Hasegos}
     */
    private static final Pattern GH_URL = Pattern.compile(
     "^https?://(?:www\\.)?github\\.com/([A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?)(?:[/?#].*)?$",
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
            throw new IllegalArgumentException("GitHub URL/사용자 명이 없습니다.");
        }
        String trimmed =  input.trim();

        /* username 만 있는 경우 */
        if(!trimmed.startsWith("http")){
            if(!GH_USER.matcher(trimmed).matches()){
                throw new IllegalArgumentException("올바른 Github 사용자명이 아닙니다.");
            }
            return trimmed;
        }

        /* URL로 입력 받은 경우 */
        Matcher m = GH_URL.matcher(trimmed);
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
     * 특정 GitHub 사용자의 공개 레포지토리 목록을 페이지네이션하여 조회합니다.
     * <p>
     * GitHub API의 {@code /users/{username}/repos} 엔드포인트를 호출하며,
     * 요청된 페이지의 데이터만 가져옵니다.
     * </p>
     * @param username    GitHub 사용자명
     * @param pageable    페이지네이션 정보 (page,size)
     * @return {@link GithubRepoDTO} 리스트 (공개 레포만)
     * @throws RuntimeException API 호출 실패 시
     */
    @Cacheable(value = "ghRepos", key = "#username + ':' + #pageable.pageNumber + ':' +  #pageable.pageSize", sync = true)
    public Page<GithubRepoDTO> getOwnedRepos(String username, Pageable pageable){

        GithubUserDTO user = getUser(username);
        long total = (user != null && user.getPublicRepos() != null)
                ? user.getPublicRepos()
                : 0;

        int currentPage = pageable.getPageNumber() + 1;

        List<GithubRepoDTO> repos = githubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{username}/repos")
                        .queryParam("type", "owner")
                        .queryParam("sort", "pushed")
                        .queryParam("per_page",pageable.getPageSize())
                        .queryParam("page", currentPage)
                        .build(username))
                .retrieve()
                .bodyToFlux(GithubRepoDTO.class)
                .collectList()
                .timeout(TIMEOUT)
                .block();

        return new PageImpl<>(repos, pageable, total);
    }


    @Cacheable(value = "ghSearch", key = "'lang:' + #language + ':page:' + #pageable.pageNumber" +
            " + ':' + #pageable.pageSize ")
   public Page<GithubRepoDTO> searchPublicRepos(String language, Pageable pageable){

        String baseQuery = "is:public";
        if(language != null && ! language.isBlank()){
            baseQuery += " language:" + language;
        }
        final String finalQuery = baseQuery;

        GithubSearchResponse<GithubRepoDTO> response = githubWebClient.get()
                .uri(uriBuilder ->  uriBuilder
                        .path("/search/repositories")
                        .queryParam("q", finalQuery)
                        .queryParam("sort", "stars")
                        .queryParam("order", "desc")
                        .queryParam("per_page", pageable.getPageSize())
                        .queryParam("page", pageable.getPageNumber() + 1)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<GithubSearchResponse<GithubRepoDTO>>() {})
                .timeout(TIMEOUT)
                .block();

        if(response == null){
            return Page.empty(pageable);
        }
        return new PageImpl<>(response.getItems(), pageable, response.getTotalCount());
    }
}