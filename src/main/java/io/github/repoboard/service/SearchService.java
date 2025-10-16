package io.github.repoboard.service;

import io.github.repoboard.common.util.SanitizeUtil;
import io.github.repoboard.dto.github.GithubRepoDTO;
import io.github.repoboard.model.Profile;
import io.github.repoboard.model.SavedRepo;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * <strong>검색 관련 기능을 제공하는 서비스</strong>
 *
 * <p>저장소 검색, 유저 프로필 조회, 저장소 목록 조회 등을 처리하며<br>
 * 컨트롤러와 하위 서비스(Profile, GitHub API, SavedRepo) 사이의 중간 계층 역할을 한다.</p>
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private final GitHubApiService gitHubApiService;
    private final ProfileService profileService;
    private final SavedRepoService savedRepoService;

    /**
     * 검색 타입에 따른 리다이렉트 경로를 위한 쿼리 문자열을 인코딩한다.
     *
     * <p>입력값은 {@link SanitizeUtil#sanitizeQuery(String)}로 정제 후
     * URL 안전하게 인코딩된다.</p>
     *
     * @param type  검색 타입 (예: "users" 또는 "repositories")
     * @param query 사용자 입력 검색어
     * @return URL 인코딩된 안전한 검색어
     */
    public String resolveRedirect(String type,String query){
        String safeQuery = SanitizeUtil.sanitizeQuery(query);
        return URLEncoder.encode(safeQuery, StandardCharsets.UTF_8);
    }

    /**
     * GitHub 저장소 검색 결과를 조회한다.
     *
     * <p>검색어는 {@link SanitizeUtil#sanitizeQuery(String)}로 정제되며,
     * 정제된 검색어를 GitHub API에 전달한다.</p>
     *
     * @param search 검색어
     * @param page   페이지 번호 (0부터 시작)
     * @param sort   정렬 기준 (예: "popular", "recent", "stars")
     * @return 검색된 저장소 목록 (페이지네이션 결과)
     */
    public Page<GithubRepoDTO> fetchRepositories(String search, int page, String sort, HttpSession session){
        String safeQuery = SanitizeUtil.sanitizeQuery(search);
        Pageable pageable = PageRequest.of(page, 50);
        return gitHubApiService.fetchReposByQuery(safeQuery, pageable, sort,session);
    }

    /**
     * GitHub 저장소 검색 결과의 추가 페이지를 조회한다.
     *
     * <p>무한 스크롤 API 요청에서 사용되며,
     * 내부 로직은 {@link #fetchRepositories(String, int, String,HttpSession)}와 동일하다.</p>
     *
     * @param search 검색어
     * @param page   페이지 번호 (0부터 시작)
     * @param sort   정렬 기준
     * @return 검색된 저장소 목록 (추가 페이지 결과)
     */
    public Page<GithubRepoDTO> loadMoreRepositories(String search, int page, String sort,HttpSession session){
        String safeQuery = SanitizeUtil.sanitizeQuery(search);
        Pageable pageable = PageRequest.of(page, 50);
        return gitHubApiService
                .fetchReposByQuery(safeQuery, pageable, sort,session);
    }

    /**
     * GitHub 로그인 이름으로 등록된 프로필을 조회한다.
     *
     * @param query GitHub 로그인 이름 (검색어)
     * @return 프로필(Optional), 존재하지 않을 경우 empty 반환
     */
    public Optional<Profile> findProfileByGithubLogin(String query){
        String safeQuery = SanitizeUtil.sanitizeQuery(query);
        return profileService.findProfileByGithubLogin(safeQuery);
    }

    /**
     * 특정 사용자가 저장한 레포지토리 목록을 조회한다.
     *
     * <p>언어, 정렬 기준, 페이지 번호, 페이지 크기에 따라 필터링 및 페이징이 적용된다.</p>
     *
     * @param userId   사용자 ID
     * @param language 언어 필터 (null 가능)
     * @param sort     정렬 기준
     * @param page     페이지 번호 (0부터 시작)
     * @param size     페이지 크기
     * @return 저장된 레포지토리 목록 (페이지네이션 결과)
     */
    public Page<SavedRepo> fetchSavedRepos(Long userId, String language, String sort, int page, int size){
        Pageable pageable = PageRequest.of(page,size);
        return savedRepoService.findAllSavedRepos(userId, language, sort, pageable);
    }
}