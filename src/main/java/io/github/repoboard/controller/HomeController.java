package io.github.repoboard.controller;

import io.github.repoboard.dto.GithubRepoDTO;
import io.github.repoboard.model.User;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.GitHubApiService;
import io.github.repoboard.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final UserService userService;
    private final GitHubApiService gitHubApiService;

    @GetMapping
    public String showhome(@AuthenticationPrincipal CustomUserPrincipal principal,
                           @RequestParam(required = false, defaultValue = "java") String language,
                           @RequestParam(defaultValue = "0") int page,
                           Model model){
        if(principal != null){
            model.addAttribute("user", principal.getUser());
        }
        Pageable finalPageable = PageRequest.of(page, 50);
        Page<GithubRepoDTO> repoPage = gitHubApiService.searchPublicRepos(language,finalPageable);
        model.addAttribute("repoPage", repoPage);
        model.addAttribute("currentLanguage", language);

        return "home";
    }

    @GetMapping("/more")
    public String loadMoreRepositories(@RequestParam(required = false, defaultValue = "java") String language,
                                       @RequestParam(defaultValue = "1") int page,
                                       Model model){

        Pageable pageable = PageRequest.of(page, 50);
        Page<GithubRepoDTO> repoPage = gitHubApiService.searchPublicRepos(language,pageable);
        model.addAttribute("repoPage", repoPage);
        model.addAttribute("currentLanguage", language);

        return "fragments/repo-card :: repo-cards";
    }
}