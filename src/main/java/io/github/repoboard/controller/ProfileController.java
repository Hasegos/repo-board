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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

import java.util.List;
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
                              @RequestParam(value = "url" , required = false) String url,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "6") int size,
                              @RequestParam(defaultValue = "all") String type,
                              Model model){
        User user = userService.findByUsername(principal.getUser().getUsername())
                .orElseThrow(() -> new EntityNotFoundException("사용자가 존재하지않습니다."));

        Optional<Profile> profile = profileService.findProfileByUserId(user.getId());

        try{
            String username;
            if(UserProvider.GITHUB == principal.getUser().getProvider()) {
                username = String.valueOf(principal.getAttributes().get("login"));

            }
            else {
                if (url == null || url.isBlank()) {
                    throw new IllegalArgumentException("Github url이 필요합니다.");
                }
                username = gitHubApiService.extractUsername(url);
            }

            GithubUserDTO userDTO = gitHubApiService.getUser(username);
            if(profile.isEmpty()){
                profileService.registerProfile(user.getId(),userDTO);
            }

            List<GithubRepoDTO> allRepos = gitHubApiService.getOwnedRepos(username, true);
            List<GithubRepoDTO> filteredRepos = allRepos.stream()
                                .filter(repo -> switch (type){
                                    case "original" -> !repo.getFork();
                                    case "fork" -> repo.getFork();
                                    default -> true;
                                })
                                .toList();

            int total = filteredRepos.size();
            int totalPages = (int) Math.ceil(total / (double) size);
            if(totalPages > 0 && page >= totalPages) page = totalPages -1;

            Pageable pageable = PageRequest.of(page, size);
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), total);
            List<GithubRepoDTO> content = (start >= total) ? List.of() : filteredRepos.subList(start, end);
            Page<GithubRepoDTO> repoPage = new PageImpl<>(content, pageable, filteredRepos.size());

            model.addAttribute("profile", userDTO);
            model.addAttribute("repos",repoPage);
            model.addAttribute("currentType",type);
        }
        catch (IOException e){
            model.addAttribute("error", e.getMessage());
        }

        model.addAttribute("user", user);

        return "profile/profile";
    }
}