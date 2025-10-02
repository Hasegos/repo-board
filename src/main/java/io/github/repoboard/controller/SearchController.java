package io.github.repoboard.controller;

import io.github.repoboard.common.util.SanitizeUtil;
import io.github.repoboard.dto.github.GithubRepoDTO;
import io.github.repoboard.model.Profile;
import io.github.repoboard.model.SavedRepo;
import io.github.repoboard.model.User;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public String redirectSearch(@RequestParam("type") String type,
                                 @RequestParam("q") String query){
        String encode = searchService.resolveRedirect(type, query);
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
        Page<GithubRepoDTO> repoPage =  searchService.fetchRepositories(search, page, sort);

        model.addAttribute("repoPage", repoPage);
        model.addAttribute("query", SanitizeUtil.sanitizeQuery(search));
        model.addAttribute("sort", sort);
        return "search/search";
    }

    @GetMapping("/repositories/api/repos")
    public String loadMoreRepositories(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam("sort") String sort,
                                       @RequestParam("q") String search,
                                       Model model){
        Page<GithubRepoDTO> repoPage = searchService.loadMoreRepositories(search, page, sort);
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
        Optional<Profile> profile = searchService.findProfileByGithubLogin(search);

        if(profile.isEmpty()){
            model.addAttribute("errorMessage", "해당 유저의 프로필이 존재하지않습니다.");
            model.addAttribute("search", SanitizeUtil.sanitizeQuery(search));
            return "search/search-user";
        }

        User user = profile.get().getUser();
        Page<SavedRepo> savedRepo = searchService.fetchSavedRepos(user.getId(),language,sort, page, size);

        model.addAttribute("savedRepos", savedRepo);
        model.addAttribute("profile", profile.get());
        model.addAttribute("sort", sort);
        model.addAttribute("language", language);
        model.addAttribute("search", SanitizeUtil.sanitizeQuery(search));

        return "search/search-user";
    }
}