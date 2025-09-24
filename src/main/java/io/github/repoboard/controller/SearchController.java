package io.github.repoboard.controller;

import io.github.repoboard.dto.github.GithubRepoDTO;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.GitHubApiService;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/search/repositories")
@RequiredArgsConstructor
public class SearchController {

    private final GitHubApiService gitHubApiService;

    @GetMapping
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
        Pageable pageable = PageRequest.of(page, 50);
        Page<GithubRepoDTO> repoPage = gitHubApiService.fetchReposByQuery(search, pageable, sort);

        model.addAttribute("repoPage", repoPage);
        model.addAttribute("query", search);
        model.addAttribute("sort", sort);
        return "search/search";
    }

    @GetMapping("/api/repos")
    public String loadMoreRepositories(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam("sort") String sort,
                                       @RequestParam("q") String search,
                                       Model model){
        Pageable pageable = PageRequest.of(page, 50);
        Page<GithubRepoDTO> repoPage = gitHubApiService.fetchReposByQuery(search, pageable, sort);
        model.addAttribute("repoPage", repoPage);
        return "fragments/repo_card :: repo-cards";
    }
}