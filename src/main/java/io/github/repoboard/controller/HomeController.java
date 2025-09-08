package io.github.repoboard.controller;

import io.github.repoboard.common.util.QueryStrategyHolder;
import io.github.repoboard.dto.GithubRepoDTO;
import io.github.repoboard.dto.QueryStrategyDTO;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.GitHubApiService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final QueryStrategyHolder queryStrategyHolder;
    private final GitHubApiService gitHubApiService;

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

        QueryStrategyDTO strategy = null;
        if (refresh) {
            strategy = queryStrategyHolder.getNextStrategy();
            session.setAttribute("refreshStrategy", strategy);
        } else {
            strategy = (QueryStrategyDTO) session.getAttribute("refreshStrategy");
        }
        Pageable finalPageable = PageRequest.of(page, 50);
        Page<GithubRepoDTO> repoPage = gitHubApiService.fetchRepos(language,finalPageable, strategy, sort);

        model.addAttribute("repoPage", repoPage);
        model.addAttribute("currentLanguage", language);
        model.addAttribute("sort", sort);

        return "home";
    }

    @GetMapping("/api/repos")
    public String loadMoreRepositories(@RequestParam(required = false, defaultValue = "java") String language,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam("sort") String sort,
                                       HttpSession session,
                                       Model model){
        QueryStrategyDTO strategy = (QueryStrategyDTO) session.getAttribute("refreshStrategy");
        Pageable pageable = PageRequest.of(page, 50);
        Page<GithubRepoDTO> repoPage = gitHubApiService.fetchRepos(language,pageable, strategy,sort);
        model.addAttribute("repoPage", repoPage);

        return "fragments/repo-card :: repo-cards";
    }
}