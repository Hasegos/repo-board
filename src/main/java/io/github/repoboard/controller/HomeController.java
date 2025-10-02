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

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

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
        Page<GithubRepoDTO> repoPage = homeService.getMoreRepos(language, sort, page, session);
        model.addAttribute("repoPage", repoPage);

        return "fragments/repo_card :: repo-cards";
    }
}