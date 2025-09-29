package io.github.repoboard.controller;

import io.github.repoboard.dto.github.GithubRepoDTO;
import io.github.repoboard.model.Profile;
import io.github.repoboard.model.SavedRepo;
import io.github.repoboard.model.User;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.GitHubApiService;
import io.github.repoboard.service.ProfileService;
import io.github.repoboard.service.SavedRepoService;
import io.github.repoboard.service.UserService;
import jakarta.persistence.EntityNotFoundException;
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
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final GitHubApiService gitHubApiService;
    private final UserService userService;
    private final ProfileService profileService;
    private final SavedRepoService savedRepoService;

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
        Pageable pageable = PageRequest.of(page, 50);
        Page<GithubRepoDTO> repoPage = gitHubApiService.fetchReposByQuery(search, pageable, sort);

        model.addAttribute("repoPage", repoPage);
        model.addAttribute("query", search);
        model.addAttribute("sort", sort);
        return "search/search";
    }

    @GetMapping("/repositories/api/repos")
    public String loadMoreRepositories(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam("sort") String sort,
                                       @RequestParam("q") String search,
                                       Model model){
        Pageable pageable = PageRequest.of(page, 50);
        Page<GithubRepoDTO> repoPage = gitHubApiService.fetchReposByQuery(search, pageable, sort);
        model.addAttribute("repoPage", repoPage);
        return "fragments/repo_card :: repo-cards";
    }

    @GetMapping("/users")
    public String showSearchUsers(@AuthenticationPrincipal CustomUserPrincipal principal,
                                  @RequestParam(required = false) String language,
                                  @RequestParam(defaultValue = "recent") String sort,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "6") int size,
                                  @RequestParam("q") String query,
                                  Model model){

        User user = userService.findByUsername(query)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저는 존재하지않습니다."));

        Profile profile = profileService.findProfileByUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("해당 유저의 프로필이 존재하지않습니다."));

        Pageable pageable = PageRequest.of(page,size);
        Page<SavedRepo> savedRepo = savedRepoService.findAllSavedRepos(user.getId(), language, sort, pageable);

        model.addAttribute("user", principal.getUser());
        model.addAttribute("savedRepos", savedRepo);
        model.addAttribute("profile", profile);
        model.addAttribute("sort", sort);
        model.addAttribute("language", language);
        model.addAttribute("query", query);

        return "search/search-user";
    }
}