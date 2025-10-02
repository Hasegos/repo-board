package io.github.repoboard.controller;

import io.github.repoboard.dto.view.SavedRepoView;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.GitHubApiService;
import io.github.repoboard.service.SavedRepoDBService;
import io.github.repoboard.service.SavedRepoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users/saved/repos")
@RequiredArgsConstructor
public class SavedRepoController {

    private final SavedRepoService savedRepoService;
    private final SavedRepoDBService savedRepoDBService;
    private final GitHubApiService gitHubApiService;

    @GetMapping
    public String showSavedRepo(@AuthenticationPrincipal CustomUserPrincipal principal,
                                @RequestParam(required = false) String language,
                                @RequestParam(required = false, defaultValue = "popular") String sort,
                                @RequestParam(defaultValue = "0") int pinnedPage,
                                @RequestParam(defaultValue = "0") int unpinnedPage,
                                Model model){
        SavedRepoView view = savedRepoService.loadSavedRepos(principal.getUser().getId(),
                                        language, sort, pinnedPage, unpinnedPage);

        model.addAttribute("user",view.getUser());
        model.addAttribute("pinnedRepos", view.getPinnedRepos());
        model.addAttribute("unpinnedRepos", view.getUnpinnedRepos());
        model.addAttribute("languageOptions", view.getLanguageOptions());
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
        try{
            savedRepoService.savedRepoById(id, principal.getUser().getId());
        } catch (IllegalArgumentException e){
            ra.addFlashAttribute("saveError", e.getMessage());
            return "redirect:/";
        }

        return "redirect:/users/saved/repos";
    }

    @PostMapping("/delete/{repoGithubId}")
    public String deleteRepo(@AuthenticationPrincipal CustomUserPrincipal principal,
                             @PathVariable Long repoGithubId){
        savedRepoDBService.delete(repoGithubId, principal.getUser().getId());

        return "redirect:/users/saved/repos";
    }

    @PostMapping("/note/{repoGithubId}")
    public String updateNote(@AuthenticationPrincipal CustomUserPrincipal principal,
                             @PathVariable Long repoGithubId,
                             @RequestParam("note") String note){
        savedRepoDBService.updateNote(repoGithubId, principal.getUser().getId(), note);

        return "redirect:/users/saved/repos";
    }

    @PostMapping("/pin/{repoGithubId}")
    public String updatePinned(@AuthenticationPrincipal CustomUserPrincipal principal,
                               @PathVariable Long repoGithubId,
                               @RequestParam boolean isPinned){
        savedRepoDBService.updatePin(repoGithubId, principal.getUser().getId(), isPinned);

        return "redirect:/users/saved/repos";
    }
}