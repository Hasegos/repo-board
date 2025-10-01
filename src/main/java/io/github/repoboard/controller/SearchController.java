package io.github.repoboard.controller;

import io.github.repoboard.common.util.SanitizeUtil;
import io.github.repoboard.dto.github.GithubRepoDTO;
import io.github.repoboard.model.Profile;
import io.github.repoboard.model.SavedRepo;
import io.github.repoboard.model.User;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.GitHubApiService;
import io.github.repoboard.service.ProfileService;
import io.github.repoboard.service.SavedRepoService;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Controller
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final GitHubApiService gitHubApiService;
    private final ProfileService profileService;
    private final SavedRepoService savedRepoService;

    @GetMapping
    public String redirectSearch(@RequestParam("type") String type,
                                 @RequestParam("q") String query){
        String safeQuery = SanitizeUtil.sanitizeQuery(query);
        String encode = URLEncoder.encode(safeQuery, StandardCharsets.UTF_8);
        if("users".equals(type)){
            return "redirect:/search/users?q=" + encode;
        }
        return "redirect:/search/repositories?q=" + encode;
    }

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
        String safeQuery = SanitizeUtil.sanitizeQuery(search);
        Pageable pageable = PageRequest.of(page, 50);
        Page<GithubRepoDTO> repoPage = gitHubApiService.fetchReposByQuery(safeQuery, pageable, sort);

        model.addAttribute("repoPage", repoPage);
        model.addAttribute("query", safeQuery);
        model.addAttribute("sort", sort);
        return "search/search";
    }

    @GetMapping("/repositories/api/repos")
    public String loadMoreRepositories(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam("sort") String sort,
                                       @RequestParam("q") String search,
                                       Model model){
        String safeQuery = SanitizeUtil.sanitizeQuery(search);
        Pageable pageable = PageRequest.of(page, 50);
        Page<GithubRepoDTO> repoPage = gitHubApiService.fetchReposByQuery(safeQuery, pageable, sort);
        model.addAttribute("repoPage", repoPage);
        return "fragments/repo_card :: repo-cards";
    }

    @GetMapping("/users")
    public String showSearchUsers(@AuthenticationPrincipal CustomUserPrincipal principal,
                                  @RequestParam(required = false) String language,
                                  @RequestParam(defaultValue = "recent") String sort,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "6") int size,
                                  @RequestParam("q") String search,
                                  Model model){

        if(search == null || search.isBlank()){
            return "redirect:/";
        }
        if(principal != null){
            model.addAttribute("user", principal.getUser());
        }
        String safeQuery = SanitizeUtil.sanitizeQuery(search);
        Optional<Profile> profile = profileService.findProfileByGithubLogin(safeQuery);

        if(profile.isEmpty()){
            model.addAttribute("errorMessage", "해당 유저의 프로필이 존재하지않습니다.");
            model.addAttribute("search", safeQuery);
            return "search/search-user";
        }

        User user = profile.get().getUser();
        Pageable pageable = PageRequest.of(page,size);
        Page<SavedRepo> savedRepo = savedRepoService.findAllSavedRepos(user.getId(), language, sort, pageable);

        model.addAttribute("savedRepos", savedRepo);
        model.addAttribute("profile", profile.get());
        model.addAttribute("sort", sort);
        model.addAttribute("language", language);
        model.addAttribute("search", safeQuery);

        return "search/search-user";
    }
}