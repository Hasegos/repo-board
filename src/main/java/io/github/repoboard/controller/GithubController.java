package io.github.repoboard.controller;

import io.github.repoboard.dto.GithubRepoDTO;
import io.github.repoboard.dto.GithubUserDTO;
import io.github.repoboard.service.GitHubApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GithubController {

    private final GitHubApiService gitHubApiService;

    @GetMapping("/profile")
    public GithubUserDTO getUserProfile(@RequestParam("url") String url){
        String username = gitHubApiService.extractUsername(url);
        return gitHubApiService.getUser(username);
    }

    @GetMapping("/repos")
    public List<GithubRepoDTO> getOwnedRepos(
            @RequestParam("url") String url,
            @RequestParam(value = "includeForks", defaultValue = "false") boolean includeForks){
        String username = gitHubApiService.extractUsername(url);
        return gitHubApiService.getOwnedRepos(username, includeForks);
    }
}