package io.github.repoboard.controller;

import io.github.repoboard.model.SavedRepo;
import io.github.repoboard.model.User;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.GitHubApiService;
import io.github.repoboard.service.SavedRepoService;
import io.github.repoboard.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.var;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/users/saved/repos")
@RequiredArgsConstructor
public class SavedRepoController {

    private final UserService userService;
    private final SavedRepoService savedRepoService;
    private final GitHubApiService gitHubApiService;

    @GetMapping
    public String showSavedRepo(@AuthenticationPrincipal CustomUserPrincipal principal,
                                @RequestParam(required = false) String language,
                                @RequestParam(required = false, defaultValue = "popular") String sort,
                                @RequestParam(defaultValue = "0") int pinnedPage,
                                @RequestParam(defaultValue = "0") int unpinnedPage,



        model.addAttribute("user",principal.getUser());
        model.addAttribute("pinnedRepos", pinnedRepos);
        model.addAttribute("unpinnedRepos", unpinnedRepos);
        model.addAttribute("languageOptions", languageOptions);
        model.addAttribute("selectedLanguage", language);
        model.addAttribute("sort", sort);

        return "repository/saved";
    }

    @GetMapping("/{repoId}/readme")
    public ResponseEntity<String> getReadmeByRepoId(@PathVariable Long repoId){
        String readme = gitHubApiService.getReadmeById(repoId);
        return ResponseEntity.ok(readme);
    }


    @PostMapping
    public String saveRepo(@AuthenticationPrincipal CustomUserPrincipal principal,
                           @RequestParam("id") Long id,
                           RedirectAttributes ra){
        User user = userService.findByUserId(principal.getUser().getId());

        try{
            savedRepoService.savedRepoById(id, user);
        } catch (IllegalArgumentException e){
            ra.addFlashAttribute("saveError", e.getMessage());
            return "redirect:/";
        }

        return "redirect:/users/saved/repos";
    }

    @PostMapping("/delete/{repoGithubId}")
    public String deleteRepo(@AuthenticationPrincipal CustomUserPrincipal principal,
                             @PathVariable Long repoGithubId){
        Long userId = principal.getUser().getId();
        savedRepoService.deleteSavedRepoDB(repoGithubId, userId);

        return "redirect:/users/saved/repos";
    }

    @PostMapping("/note/{repoGithubId}")
    public String updateNote(@AuthenticationPrincipal CustomUserPrincipal principal,
                             @PathVariable Long repoGithubId,
                             @RequestParam("note") String note){

        Long userId = principal.getUser().getId();
        savedRepoService.updateSavedRepoNote(repoGithubId, userId, note);

        return "redirect:/users/saved/repos";
    }

    @PostMapping("/pin/{repoGithubId}")
    public String updatePinned(@AuthenticationPrincipal CustomUserPrincipal principal,
                               @PathVariable Long repoGithubId,
                               @RequestParam boolean isPinned){
        Long userId = principal.getUser().getId();
        savedRepoService.updatedSavedRepoPin(repoGithubId, userId, isPinned);

        return "redirect:/users/saved/repos";
    }
}