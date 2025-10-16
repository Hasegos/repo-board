package io.github.repoboard.controller;

import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.AdminService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 관리자 기능을 제공하는 컨트롤러입니다.
 * <p>
 * 관리자 페이지 접속, 유저 목록 조회, 유저 삭제/복구, 상태 토글, 로그 조회 등의 기능을 제공합니다.
 * </p>
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 관리자 페이지를 렌더링합니다.
     * <p>
     * 전체 유저 목록, 삭제된 유저 목록, 선택된 유저의 상세 정보를 모델에 포함시킵니다.
     * </p>
     *
     * @param principal 로그인한 관리자 정보
     * @param userId 선택된 유저 ID (선택적 파라미터)
     * @param model 뷰에 전달할 데이터 모델
     * @return admin/admin 템플릿 경로
     */
    @GetMapping
    public String showAdmin(@AuthenticationPrincipal CustomUserPrincipal principal,
                            @RequestParam(value = "userId", required = false) Long userId,
                            Model model){

        model.addAttribute("user",principal.getUser());
        model.addAttribute("users",adminService.getAllUsers());
        model.addAttribute("deletedUsers", adminService.getDeletedUsers());
        return "admin/admin";
    }

    /**
     * 관리자 로그를 페이징하여 반환합니다.
     *
     * @param page 로그 페이지 번호 (기본값: 0)
     * @return 로그 문자열 리스트
     */
    @GetMapping("/logs")
    @ResponseBody
    public List<String> getAdminLogs(@RequestParam(defaultValue = "0") int page){
        int linesPerPage = 50;
        return adminService.readAdminLogs(page, linesPerPage);
    }

    /**
     * 지정된 사용자를 삭제(소프트 삭제)합니다.
     *
     * @param userId 삭제할 사용자 ID
     * @return 관리자 페이지로 리다이렉트
     */
    @PostMapping("/delete/{userId}")
    public String deleteUser(@PathVariable Long userId,
                             RedirectAttributes ra){
        try{
            adminService.deleteUser(userId);
        }catch (EntityNotFoundException e){
            ra.addFlashAttribute("error",e.getMessage());
        }catch (Exception e){
            ra.addFlashAttribute("error", "⚠ 사용자 삭제 중 예외가 발생했습니다: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    /**
     * 최근 삭제된 사용자를 복구합니다.
     *
     * @param username 복구할 사용자명
     * @return 관리자 페이지로 리다이렉트
     */
    @PostMapping("/restore/{username}")
    public String  restoreUser(@PathVariable String username,
                               RedirectAttributes ra){
        try{
            adminService.restoreDeletedUser(username);
        }catch (IllegalArgumentException e){
            ra.addFlashAttribute("error","❌ " +  e.getMessage());
        }catch (IllegalStateException e){
            ra.addFlashAttribute("error","❌ 복구 불가: " + e.getMessage());
        }catch (Exception e){
            ra.addFlashAttribute("error","⚠ 알 수 없는 오류: "  + e.getMessage());
        }
        return "redirect:/admin";
    }

    /**
     * 사용자의 활성화 상태를 토글합니다. (ACTIVE ↔ SUSPENDED)
     *
     * @param userId 대상 사용자 ID
     * @return 관리자 페이지로 리다이렉트
     */
    @PostMapping("/toggle/{userId}")
    public String toggleUserStatus(@PathVariable Long userId,
                                   RedirectAttributes ra){
        try{
            adminService.toggleUserStatus(userId);
        }catch (EntityNotFoundException e){
            ra.addFlashAttribute("error","❌ " + e.getMessage());
        }catch (Exception e){
            ra.addFlashAttribute("error","⚠ 알 수 없는 오류: " + e.getMessage());
        }
        return "redirect:/admin";
    }
}