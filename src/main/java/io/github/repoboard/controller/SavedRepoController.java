package io.github.repoboard.controller;

import io.github.repoboard.model.SavedRepo;
import io.github.repoboard.model.User;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.GitHubApiService;
import io.github.repoboard.service.SavedRepoService;
import io.github.repoboard.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/users/saved/repos")
@RequiredArgsConstructor
public class SavedRepoController {

    private final UserService userService;
    private final SavedRepoService savedRepoService;
    private final GitHubApiService gitHubApiService;

    @GetMapping
    public String showSavedRepo(@AuthenticationPrincipal CustomUserPrincipal principal,
                                Model model){

        User user = userService.findByUserId(principal.getUser().getId());
        List<SavedRepo> savedRepos = savedRepoService.getSavedReposByUserId(user.getId());
        model.addAttribute("user",principal.getUser());
        model.addAttribute("savedRepos", savedRepos);

        return "repository/saved";
    }

    @GetMapping("/{id}/readme")
    public ResponseEntity<String> getReadmeByRepoId(@PathVariable Long id){
        String readme = gitHubApiService.getReadmeById(id);
        return ResponseEntity.ok(readme);
    }

    @PostMapping
    public String saveRepo(@AuthenticationPrincipal CustomUserPrincipal principal,
                           @RequestParam("id") Long id){

        User user = userService.findByUserId(principal.getUser().getId());
        savedRepoService.savedRepoById(id, user);

        return "redirect:/users/saved/repos";
    }
}