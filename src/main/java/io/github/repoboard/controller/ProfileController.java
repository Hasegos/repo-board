package io.github.repoboard.controller;

import io.github.repoboard.dto.view.ProfileView;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.ProfileService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/users/profiles")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public String showProfile(@AuthenticationPrincipal CustomUserPrincipal principal,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "6") int size,
                              @RequestParam(defaultValue = "all") String type,
                              Model model){
        try{
            ProfileView profileView = profileService.loadProfilePage(principal.getUser().getId(), page, size, type);
            model.addAttribute("user", profileView.getUser());
            model.addAttribute("view", profileView);
        }
        catch (Exception e){
            model.addAttribute("error", e.getMessage());
            model.addAttribute("onboarding",true);
        }
        return "profile/profile";
    }

    @PostMapping("/setup")
    public String setupProfile(@AuthenticationPrincipal CustomUserPrincipal principal,
                               @RequestParam("url") String url,
                               RedirectAttributes ra){
        try {
            profileService.setupProfile(principal.getUser().getId(), url);
            return "redirect:/users/profiles";
        }catch (IOException e){
            ra.addFlashAttribute("error", "Github API 호출 중 오류가 발생했습니다.");
            return "redirect:/users/profiles";
        }
        catch (Exception e){
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/users/profiles";
        }
    }

    @PostMapping("/refresh")
    public String refreshProfile(@AuthenticationPrincipal CustomUserPrincipal principal,
                                 RedirectAttributes ra){
        try{
            profileService.refreshProfile(principal.getUser().getId());
        }catch (EntityNotFoundException | IllegalArgumentException e){
            ra.addFlashAttribute("error", e.getMessage());
        }
        catch (IOException e){
            ra.addFlashAttribute("error", "프로필 이미지 업데이트 중 오류가 발생했습니다");
        }catch (Exception e){
            ra.addFlashAttribute("error", "프로필 새로고침 실패");
        }
        return "redirect:/users/profiles";
    }

    @PostMapping("/visibility")
    public String updateVisibility(@AuthenticationPrincipal CustomUserPrincipal principal,
                                   @RequestParam(value = "profileVisibility", required = false) String visibility,
                                   RedirectAttributes ra){
        try{
            profileService.updateProfileVisibility(principal.getUser().getId(), visibility);
        }catch (EntityNotFoundException e){
            ra.addFlashAttribute("error", e.getMessage());
        }catch (Exception e){
            ra.addFlashAttribute("error", "프로필 업데이트 실패했습니다.");
        }
        return "redirect:/users/profiles";
    }
}