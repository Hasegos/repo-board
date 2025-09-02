package io.github.repoboard.controller;

import io.github.repoboard.dto.GithubRepoDTO;
import io.github.repoboard.service.GitHubApiService;
import io.github.repoboard.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestAPIController {
    private final GitHubApiService gitHubApiService;
    private final UserService userService;

    @GetMapping
    public Page<GithubRepoDTO> searchRepositories(@RequestParam(required = false, defaultValue = "java") String language,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "100") int size){
        Pageable pageable = PageRequest.of(page,size);
        return gitHubApiService.searchPublicRepos(language,pageable);
    }
}
