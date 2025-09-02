package io.github.repoboard.controller;

import io.github.repoboard.dto.GithubRepoDTO;
import io.github.repoboard.model.User;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.GitHubApiService;
import io.github.repoboard.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/search/repositories")
@RequiredArgsConstructor
public class SearchController {

    private final GitHubApiService gitHubApiService;
    private final UserService userService;

    @GetMapping
    public String showSearchPage(@AuthenticationPrincipal CustomUserPrincipal principal,
                                 @RequestParam(required = false, defaultValue = "java") String language,
                                 @RequestParam(defaultValue = "0") int page,
                                 Model model){

        User user = userService.findByUserId(principal.getUser().getId());
        model.addAttribute("user", user);

        List<GithubRepoDTO> allContent = new ArrayList<>();
        Page<GithubRepoDTO> lastPageResult = Page.empty();

        for(int i = 0; i<= page; i++){
            Pageable currentPageable =  PageRequest.of(i, 100);
            lastPageResult = gitHubApiService.searchPublicRepos(language, currentPageable);
            allContent.addAll(lastPageResult.getContent());
        }

        Pageable finalPageable = PageRequest.of(page, 100 * (page + 1));
        Page<GithubRepoDTO> repoPage = new PageImpl<>(allContent,finalPageable,lastPageResult.getTotalElements());

        model.addAttribute("repoPage", repoPage);
        model.addAttribute("currentLanguage", language);
        return "repository/list";
    }

    @GetMapping("/more")
    public String loadMoreRepositories(@RequestParam(required = false, defaultValue = "java") String language,
                                       @RequestParam(defaultValue = "1") int page,
                                       Model model){

        Pageable pageable = PageRequest.of(page, 100);
        Page<GithubRepoDTO> repoPage = gitHubApiService.searchPublicRepos(language,pageable);
        model.addAttribute("repoPage", repoPage);
        model.addAttribute("currentLanguage", language);

        return "repository/repo-card :: repo-cards";
    }
}