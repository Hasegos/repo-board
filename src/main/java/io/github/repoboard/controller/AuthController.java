package io.github.repoboard.controller;

import io.github.repoboard.dto.auth.UserDTO;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.UserService;
import jakarta.persistence.EntityExistsException;
import jakarta.servlet.http.HttpSession;
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

/**
 * 사용자 인증 관련 기능을 제공하는 컨트롤러입니다.
 *
 * <p>
 * 주로 회원가입 및 로그인 페이지 렌더링을 처리하며,<br>
 * 폼 제출 시 유효성 검사와 예외 처리도 담당합니다.
 * </p>
 */
@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 회원가입 페이지를 보여줍니다.
     * <p>
     * 이미 로그인된 경우 메인 페이지로 리다이렉트됩니다.
     * </p>
     *
     * @param principal 현재 로그인 사용자 정보 (null이면 비로그인 상태)
     * @param model Thymeleaf 모델
     * @return 회원가입 페이지 경로 또는 메인 리다이렉트
     */
    @GetMapping("/signup")
    public String showRegister(@AuthenticationPrincipal CustomUserPrincipal principal,
                               Model model){
        if(principal != null){
            return "redirect:/";
        }
        model.addAttribute("userDTO", new UserDTO());
        return "auth/signup";
    }

    /**
     * 회원가입 요청을 처리합니다.
     *
     * @param dto 사용자 입력 정보
     * @param br 유효성 검사 결과
     * @param model Thymeleaf 모델
     * @return 성공 시 로그인 페이지, 실패 시 다시 회원가입 페이지 렌더링
     */
    @PostMapping("/signup")
    public String postRegister(@ModelAttribute("userDTO") @Valid UserDTO dto,
                               BindingResult br,
                               Model model){
        try {
            if (userService.findByUsername(dto.getUsername()).isPresent()) {
                br.rejectValue("username", "duplicate", "이미 존재하는 회원입니다.");
                return "auth/signup";
            }
            if (!br.hasFieldErrors("password") && !br.hasFieldErrors("passwordConfirm")) {
                if (!java.util.Objects.equals(dto.getPassword(), dto.getPasswordConfirm())) {
                    br.rejectValue("passwordConfirm", "password.mismatch", "비밀번호가 일치하지 않습니다.");
                }
            }
            if (br.hasErrors()) {
                return "auth/signup";
            }
            userService.register(dto);
            return "redirect:/auth/login";
        }catch (EntityExistsException e){
            br.rejectValue("username", "duplicate", e.getMessage());
            return "auth/signup";
        } catch (IllegalStateException e){
            model.addAttribute("error",e.getMessage());
            return "auth/signup";
        }catch (Exception e){
            model.addAttribute("error", "⚠ 알 수 없는 오류가 발생했습니다: " + e.getMessage());
            return "auth/signup";
        }
    }

    /**
     * 로그인 페이지를 보여줍니다.
     * <p>
     * 이미 로그인된 경우 메인 페이지로 리다이렉트됩니다.<br>
     * 세션에 저장된 로그인 에러 메시지도 모델에 전달합니다.
     * </p>
     *
     * @param principal 현재 로그인 사용자 정보
     * @param model Thymeleaf 모델
     * @param httpSession 세션 객체
     * @return 로그인 페이지 또는 메인 리다이렉트
     */
    @GetMapping("/login")
    public String showLogin(@AuthenticationPrincipal CustomUserPrincipal principal,
                            Model model, HttpSession httpSession){
        if(principal != null){
            return "redirect:/";
        }
        Object msg = httpSession.getAttribute("loginError");
        if(msg != null){
            model.addAttribute("loginError", msg);
            httpSession.removeAttribute("loginError");
        }
        return "auth/login";
    }
}