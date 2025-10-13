package io.github.repoboard.controller;

import io.github.repoboard.dto.view.ProfileView;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.ProfileDBService;
import io.github.repoboard.service.ProfileService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

/**
 * 사용자 프로필 관련 기능을 처리하는 컨트롤러입니다.
 *
 * <p>
 * GitHub 프로필 연동, 새로고침, 공개 여부 설정 등<br>
 * 사용자 프로필 데이터의 표시와 수정 기능을 제공합니다.
 * </p>
 */
@Controller
@RequestMapping("/users/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileDBService profileDBService;

    /**
     * 사용자 프로필 관련 기능을 처리하는 컨트롤러입니다.
     *
     * <p>
     * GitHub 프로필 연동, 새로고침, 공개 여부 설정 등<br>
     * 사용자 프로필 데이터의 표시와 수정 기능을 제공합니다.
     * </p>
     */
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

    /**
     * 사용자의 GitHub 프로필 URL을 등록하여 초기 설정을 수행합니다.
     *
     * @param principal 로그인 사용자 정보
     * @param url GitHub 프로필 URL
     * @param ra 리다이렉트 시 전달할 플래시 속성
     * @return 프로필 페이지로 리다이렉트
     */
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

    /**
     * 사용자의 GitHub 프로필을 새로고침합니다.
     *
     * <p>
     * API를 통해 최신 프로필 정보와 이미지를 갱신합니다.
     * </p>
     *
     * @param principal 로그인 사용자 정보
     * @param ra 리다이렉트 시 전달할 플래시 속성
     * @return 프로필 페이지로 리다이렉트
     */
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

    /**
     * 프로필 공개 여부(PUBLIC/PRIVATE)를 변경합니다.
     *
     * @param principal 로그인 사용자 정보
     * @param visibility 새로 설정할 공개 상태 값
     * @param ra 리다이렉트 시 전달할 플래시 속성
     * @return 프로필 페이지로 리다이렉트
     */
    @PostMapping("/visibility")
    public String updateVisibility(@AuthenticationPrincipal CustomUserPrincipal principal,
                                   @RequestParam(value = "profileVisibility", required = false) String visibility,
                                   RedirectAttributes ra){
        try{
            profileDBService.updateProfileVisibility(principal.getUser().getId(), visibility);
        }catch (EntityNotFoundException e){
            ra.addFlashAttribute("error", e.getMessage());
        }catch (Exception e){
            ra.addFlashAttribute("error", "프로필 업데이트 실패했습니다.");
        }
        return "redirect:/users/profiles";
    }
}