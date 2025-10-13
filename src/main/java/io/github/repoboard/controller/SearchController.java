package io.github.repoboard.controller;

import io.github.repoboard.common.util.SanitizeUtil;
import io.github.repoboard.dto.github.GithubRepoDTO;
import io.github.repoboard.model.Profile;
import io.github.repoboard.model.SavedRepo;
import io.github.repoboard.model.User;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * 검색 기능을 담당하는 컨트롤러입니다.
 *
 * <p>레포지토리 및 오픈 프로필 유저 검색을 지원하며,
 * 페이지 진입 시 자동 리다이렉트 또는 Fragment 응답을 제공합니다.</p>
 */
@Controller
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 검색 유형에 따라 적절한 검색 결과 페이지로 리다이렉트합니다.
     *
     * @param type 검색 유형 (예: "users", "repositories")
     * @param query 검색어
     * @return 리다이렉트 URL
     */
    @GetMapping
    public String redirectSearch(@RequestParam("type") String type,
                                 @RequestParam("q") String query){
        String encode = searchService.resolveRedirect(type, query);
        if("users".equals(type)){
            return "redirect:/search/users?q=" + encode;
        }
        return "redirect:/search/repositories?q=" + encode;
    }

    /**
     * 저장소 검색 결과 페이지를 렌더링합니다.
     *
     * @param principal 로그인 사용자 정보
     * @param search 검색어
     * @param page 페이지 번호 (기본값: 0)
     * @param sort 정렬 기준 (예: popular, recent)
     * @param model 뷰 모델
     * @return 저장소 검색 결과 페이지
     */
    @GetMapping("/repositories")
    public String showSearch(@AuthenticationPrincipal CustomUserPrincipal principal,
                             @RequestParam("q") String search,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(value = "sort" , required = false , defaultValue = "popular") String sort,
                             Model model){
        if(search == null || search.isBlank()){
            return "redirect:/";
        }
        if(principal != null){
            model.addAttribute("user", principal.getUser());
        }
        Page<GithubRepoDTO> repoPage =  searchService.fetchRepositories(search, page, sort);

        model.addAttribute("repoPage", repoPage);
        model.addAttribute("query", SanitizeUtil.sanitizeQuery(search));
        model.addAttribute("sort", sort);
        return "search/search";
    }

    /**
     * 저장소 검색 결과의 다음 페이지를 Fragment로 반환합니다.
     * (무한 스크롤/페이징에 사용)
     *
     * @param page 요청할 페이지 번호
     * @param sort 정렬 기준
     * @param search 검색어
     * @param model 뷰 모델
     * @return 저장소 카드 fragment (HTML 일부)
     */
    @GetMapping("/repositories/api/repos")
    public String loadMoreRepositories(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam("sort") String sort,
                                       @RequestParam("q") String search,
                                       Model model){
        Page<GithubRepoDTO> repoPage = searchService.loadMoreRepositories(search, page, sort);
        model.addAttribute("repoPage", repoPage);
        return "fragments/repo_card :: repo-cards";
    }

    /**
     * 특정 깃허브 로그인 이름으로 오픈 프로필 유저 검색 결과를 렌더링합니다.
     *
     * @param principal 로그인 사용자 정보
     * @param language 언어 필터
     * @param sort 정렬 기준 (기본값: recent)
     * @param page 페이지 번호
     * @param size 페이지당 개수
     * @param search 검색어 (GitHub 로그인 ID)
     * @param model 뷰 모델
     * @return 유저 오픈 프로필 검색 결과 페이지
     */
    @GetMapping("/users")
    public String showSearchUsers(@AuthenticationPrincipal CustomUserPrincipal principal,
                                  @RequestParam(required = false) String language,
                                  @RequestParam(defaultValue = "recent") String sort,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "6") int size,
                                  @RequestParam("q") String search,
                                  Model model){

        if(search == null || search.isBlank()){
            return "redirect:/";
        }
        if(principal != null){
            model.addAttribute("user", principal.getUser());
        }
        Optional<Profile> profile = searchService.findProfileByGithubLogin(search);

        if(profile.isEmpty()){
            model.addAttribute("errorMessage", "해당 유저의 프로필이 존재하지않습니다.");
            model.addAttribute("search", SanitizeUtil.sanitizeQuery(search));
            return "search/search-user";
        }

        User user = profile.get().getUser();
        Page<SavedRepo> savedRepo = searchService.fetchSavedRepos(user.getId(),language,sort, page, size);

        model.addAttribute("savedRepos", savedRepo);
        model.addAttribute("profile", profile.get());
        model.addAttribute("sort", sort);
        model.addAttribute("language", language);
        model.addAttribute("search", SanitizeUtil.sanitizeQuery(search));

        return "search/search-user";
    }
}