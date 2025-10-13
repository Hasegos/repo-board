package io.github.repoboard.controller;

import io.github.repoboard.dto.request.ChangePasswordDTO;
import io.github.repoboard.model.User;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.SettingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 사용자 설정 관련 기능을 처리하는 컨트롤러입니다.
 * <p>
 * 지원 기능:
 * <ul>
 *     <li>설정 페이지 진입</li>
 *     <li>비밀번호 변경</li>
 *     <li>회원 탈퇴</li>
 * </ul>
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/users/settings")
public class SettingController {

    private final SettingService settingService;

    /**
     * 사용자 설정 페이지를 보여줍니다.
     *
     * @param principal 로그인 사용자 정보
     * @param model     뷰에 전달할 모델
     * @return 설정 페이지 뷰 이름
     */
    @GetMapping
    public String showSettings(@AuthenticationPrincipal CustomUserPrincipal principal,
                               Model model){

        User user = settingService.getUserByPrincipal(principal);

        if (!model.containsAttribute("ChangePasswordDTO")) {
            model.addAttribute("ChangePasswordDTO", new ChangePasswordDTO());
        }

        model.addAttribute("authType", user.getProvider().name());
        model.addAttribute("user", principal.getUser());
        return "settings/settings";
    }

    /**
     * 사용자의 비밀번호를 변경합니다.
     * <p>
     * 성공 시 로그아웃 처리 후 로그인 페이지로 이동합니다.
     * 실패 시 에러 메시지와 함께 설정 페이지로 리다이렉트됩니다.
     *
     * @param principal 로그인 사용자
     * @param change    비밀번호 변경 DTO
     * @param br        유효성 검사 결과
     * @param request   HTTP 요청
     * @param response  HTTP 응답
     * @param ra        리다이렉트 시 플래시 속성 전달
     * @return 리다이렉트 경로
     */
    @PostMapping("/password")
    public String changeUserPassword(@AuthenticationPrincipal CustomUserPrincipal principal,
                                     @Valid @ModelAttribute("ChangePasswordDTO") ChangePasswordDTO change,
                                     BindingResult br,
                                     HttpServletRequest request,
                                     HttpServletResponse response,
                                     RedirectAttributes ra){
        settingService.changeUserPassword(principal.getUser().getId(), change, br);

        if(br.hasErrors()) {
            ra.addFlashAttribute("ChangePasswordDTO", change);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.ChangePasswordDTO", br);
            return "redirect:/users/settings";
        }

        settingService.logoutAfterPasswordChange(request,response);
        return "redirect:/users/login";
    }

    /**
     * 현재 사용자의 계정을 탈퇴 처리합니다.
     * <p>
     * 성공 시 세션 종료 후 홈으로 리다이렉트되며,
     * 실패 시 에러 메시지를 포함하여 설정 페이지로 이동합니다.
     *
     * @param principal 로그인 사용자 정보
     * @param request   HTTP 요청
     * @param response  HTTP 응답
     * @return 리다이렉트 경로
     */
    @PostMapping("/delete")
    public String deleteUser(@AuthenticationPrincipal CustomUserPrincipal principal,
                             HttpServletRequest request,
                             HttpServletResponse response){
        try{
            settingService.deleteUser(principal.getUser().getId(), request, response);
            return "redirect:/";
        }catch (Exception e){
            return "redirect:/users/settings?error=" + e.getMessage();
        }
    }
}