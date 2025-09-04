package io.github.repoboard.controller;

import io.github.repoboard.dto.GithubRepoDTO;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.GitHubApiService;
import io.github.repoboard.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final UserService userService;
    private final GitHubApiService gitHubApiService;

    @GetMapping("/")
    public String showhome(@AuthenticationPrincipal CustomUserPrincipal principal,
                           @RequestParam(value = "language", required = false, defaultValue = "java") String language,
                           @RequestParam(value = "refresh", required = false, defaultValue = "false") boolean refresh,
                           @RequestParam(defaultValue = "0") int page,
                           HttpServletResponse response,
                           Model model) throws IOException {
        if(principal != null){
            model.addAttribute("user", principal.getUser());
        }

        Pageable finalPageable = PageRequest.of(page, 50);
        Page<GithubRepoDTO> repoPage = gitHubApiService.fetchRepos(language,finalPageable, refresh);
        model.addAttribute("repoPage", repoPage);
        model.addAttribute("currentLanguage", language);

        if (refresh) {
            response.sendRedirect("/?language=" + language + "&page=" + page);
            return null;
        }

        return "home";
    }

    @GetMapping("/api/repos")
    public String loadMoreRepositories(@RequestParam(required = false, defaultValue = "java") String language,
                                       @RequestParam(required = false, defaultValue = "false") boolean refresh,
                                       @RequestParam(defaultValue = "1") int page,
                                       Model model){

        Pageable pageable = PageRequest.of(page, 50);
        Page<GithubRepoDTO> repoPage = gitHubApiService.fetchRepos(language,pageable,refresh);
        model.addAttribute("repoPage", repoPage);
        model.addAttribute("currentLanguage", language);

        return "fragments/repo-card :: repo-cards";
    }
}