package io.github.repoboard.controller;

import io.github.repoboard.dto.github.GithubRepoDTO;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.HomeService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 홈 화면 관련 요청을 처리하는 컨트롤러입니다.
 *
 * <p>
 * GitHub 인기 레포지토리를 언어, 정렬 기준에 따라 검색하여<br>
 * 홈 페이지 또는 추가 로딩 Fragment로 반환합니다.
 * </p>
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    /**
     * 홈 페이지를 렌더링합니다.
     *
     * <p>
     * 로그인된 사용자의 정보와 함께 GitHub 인기 레포지토리를 조회하여 모델에 담습니다.<br>
     * 언어(language), 정렬 기준(sort), 새로고침 여부(refresh), 페이지 번호(page)는 쿼리 파라미터로 지정됩니다.
     * </p>
     *
     * @param principal 로그인 사용자 정보 (nullable)
     * @param language 필터링할 언어 (기본값: java)
     * @param refresh true일 경우 API 캐시를 무시하고 새로 요청
     * @param sort 정렬 기준 (popular=stars, recent=updated 등)
     * @param session 사용자 세션 (API 캐싱에 사용)
     * @param page 현재 페이지 번호 (0부터 시작)
     * @param model Thymeleaf 뷰 모델
     * @return 홈 페이지 템플릿 이름 ("home")
     */
    @GetMapping("/")
    public String showhome(@AuthenticationPrincipal CustomUserPrincipal principal,
                           @RequestParam(value = "language", required = false, defaultValue = "java") String language,
                           @RequestParam(value = "refresh", required = false, defaultValue = "false") boolean refresh,
                           @RequestParam(value = "sort", required = false, defaultValue = "popular") String sort,
                           HttpSession session,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {
        if(principal != null){
            model.addAttribute("user", principal.getUser());
        }
        Page<GithubRepoDTO> repoPage = homeService.getRepos(language, sort, refresh, page, session);

        Object errorFlag = session.getAttribute("ghApiError");
        if(errorFlag != null && Boolean.TRUE.equals(errorFlag)){
            model.addAttribute("error","⚠ GitHub에서 데이터를 가져오는 데 문제가 발생했습니다. 잠시 후 다시 시도해주세요.");
            session.removeAttribute("ghApiError");
        }

        model.addAttribute("repoPage", repoPage);
        model.addAttribute("currentLanguage", language);
        model.addAttribute("sort", sort);
        return "home";
    }

    /**
     * 홈 페이지의 레포지토리 추가 목록을 비동기 로딩합니다.
     *
     * <p>
     * JavaScript의 infinite scroll에 의해 호출되며,<br>
     * 지정된 언어, 정렬 기준, 페이지에 따라 추가 레포를 조회하고<br>
     * Thymeleaf fragment로 반환합니다.
     * </p>
     *
     * @param language 필터링할 언어 (기본값: java)
     * @param page 요청할 페이지 번호 (기본값: 1)
     * @param sort 정렬 기준 (예: stars, updated 등)
     * @param session 사용자 세션 (캐시 키로 사용)
     * @param model Thymeleaf 뷰 모델
     * @return repo_card fragment (fragments/repo_card :: repo-cards)
     */
    @GetMapping("/api/repos")
    public String loadMoreRepositories(@RequestParam(required = false, defaultValue = "java") String language,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam("sort") String sort,
                                       HttpSession session,
                                       Model model){
        Page<GithubRepoDTO> repoPage = homeService.getMoreRepos(language, sort, page, session);

        Object errorFlag = session.getAttribute("ghApiError");
        if(errorFlag != null && Boolean.TRUE.equals(errorFlag)){
            model.addAttribute("error","⚠ GitHub에서 데이터를 가져오는 데 문제가 발생했습니다. 잠시 후 다시 시도해주세요.");
            session.removeAttribute("ghApiError");
        }

        model.addAttribute("repoPage", repoPage);
        return "fragments/repo_card :: repo-cards";
    }
}