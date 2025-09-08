package io.github.repoboard.controller;

import io.github.repoboard.dto.GithubRepoDTO;
import io.github.repoboard.dto.GithubUserDTO;
import io.github.repoboard.model.Profile;
import io.github.repoboard.model.User;
import io.github.repoboard.model.enums.UserProvider;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.GitHubApiService;
import io.github.repoboard.service.ProfileService;
import io.github.repoboard.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/users/profiles")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final GitHubApiService gitHubApiService;
    private final ProfileService profileService;
    private final UserService userService;

    @GetMapping
    public String showProfile(@AuthenticationPrincipal CustomUserPrincipal principal,
                              @RequestParam(value = "u", required = false) String u,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "6") int size,
                              @RequestParam(defaultValue = "all") String type,
                              Model model){
        User user = userService.findByUsername(principal.getUser().getUsername())
                .orElseThrow(() -> new EntityNotFoundException("사용자가 존재하지않습니다."));
        model.addAttribute("user", user);

        try{
            String username = null;
            Optional<Profile> profileOptional = profileService.findProfileByUserId(user.getId());

            if(profileOptional.isPresent()){
                username = profileOptional.get().getGithubLogin();
            }
            else if(UserProvider.GITHUB == principal.getUser().getProvider()) {
                username = String.valueOf(principal.getAttributes().get("login"));
            }
            else if(u != null && !u.isEmpty()){
                username = u;
            }

            if(username == null){
                model.addAttribute("onboarding",true);
                return "profile/profile";
            }

            GithubUserDTO userDTO = gitHubApiService.getUser(username);
            if (profileOptional.isEmpty()) {
                profileService.registerProfile(user.getId(), userDTO);
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<GithubRepoDTO> repoPage = profileService.loadProfileView(username,pageable,type);

            model.addAttribute("u", username);
            model.addAttribute("profile", userDTO);
            model.addAttribute("repos", repoPage);
            model.addAttribute("currentType", type);
            model.addAttribute("onboarding",false);
        }
        catch (Exception e){
            model.addAttribute("error", e.getMessage());
            model.addAttribute("onboarding",true);
        }

        return "profile/profile";
    }

    @PostMapping("/setup")
    public String setupProfile(@RequestParam("url") String url,
                               RedirectAttributes ra){

        try {
            String username = gitHubApiService.extractUsername(url);
            ra.addAttribute("u", username);
            return "redirect:/users/profiles";
        }catch (Exception e){
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/users/profiles";
        }
    }
}