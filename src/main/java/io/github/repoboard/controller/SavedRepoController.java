package io.github.repoboard.controller;

import io.github.repoboard.dto.GithubRepoDTO;
import io.github.repoboard.model.SavedRepo;
import io.github.repoboard.model.User;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.SavedRepoService;
import io.github.repoboard.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/users/saved/repos")
@RequiredArgsConstructor
public class SavedRepoController {

    private final UserService userService;
    private final SavedRepoService savedRepoService;

    @GetMapping
    public String showSavedRepo(@AuthenticationPrincipal CustomUserPrincipal principal,
                                Model model){

        User user = userService.findByUserId(principal.getUser().getId());
        List<SavedRepo> savedRepos = savedRepoService.getSavedReposByUserId(user.getId());
        model.addAttribute("user",principal.getUser());
        model.addAttribute("savedRepos", savedRepos);

        return "repository/saved";
    }


    @PostMapping
    public String saveRepo(@AuthenticationPrincipal CustomUserPrincipal principal,
                           @RequestParam Long id,
                           HttpSession session){
        List<GithubRepoDTO> cached = (List<GithubRepoDTO>) session.getAttribute("cachedRepos");
        GithubRepoDTO repo = cached.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 레포 없음"));


        return "redirect:/users/saved/repos";
    }
}