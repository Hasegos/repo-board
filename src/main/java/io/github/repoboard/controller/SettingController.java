package io.github.repoboard.controller;

import io.github.repoboard.dto.request.ChangePasswordDTO;
import io.github.repoboard.model.User;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users/settings")
public class SettingController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private OAuth2AuthorizedClientService clientService;

    @GetMapping
    public String showSettings(@AuthenticationPrincipal CustomUserPrincipal principal,
                               Model model)
    {

        User user = userService.findByUsername(principal.getUser().getUsername())
                        .orElseThrow(() -> new EntityNotFoundException("해당 유저는 존재하지 않습니다."));

        if (!model.containsAttribute("ChangePasswordDTO")) {
            model.addAttribute("ChangePasswordDTO", new ChangePasswordDTO());
        }

        model.addAttribute("authType", user.getProvider().name());
        model.addAttribute("user", principal.getUser());
        return "settings/settings";
    }

    @PostMapping("/password")
    public String changeUserPassword(@AuthenticationPrincipal CustomUserPrincipal principal,
                                     @Valid @ModelAttribute("ChangePasswordDTO") ChangePasswordDTO change,
                                     BindingResult br,
                                     HttpServletRequest request,
                                     HttpServletResponse response,
                                     RedirectAttributes ra)
    {
        try{
            userService.changeUserPassword(principal.getUser().getId(), change);
        }catch (BadCredentialsException e) {
            br.rejectValue("currentPassword", "bad", "현재 비밀번호가 올바르지 않습니다.");
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("동일")) {
                br.rejectValue("newPassword", "same", e.getMessage());
            }
            else if (e.getMessage().contains("확인")) {
                br.rejectValue("confirmPassword", "mismatch", e.getMessage());
            }
            else {
                br.reject("error", e.getMessage());
            }
        } catch (AccessDeniedException e) {
            br.reject("denied", "소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
        }

        if(br.hasErrors()) {
            ra.addFlashAttribute("ChangePasswordDTO", change);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.ChangePasswordDTO", br);
            return "redirect:/users/settings";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler()
                .logout(request, response, auth);
        return "redirect:/users/login";
    }

    @PostMapping("/delete")
    public String deleteUser(@AuthenticationPrincipal CustomUserPrincipal principal,
                             HttpServletRequest request,
                             HttpServletResponse response)
    {

        try{
            userService.deleteUserAndProfile(principal.getUser().getId());
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();


            if (auth instanceof OAuth2AuthenticationToken oat && clientService != null) {
                clientService.removeAuthorizedClient(
                        oat.getAuthorizedClientRegistrationId(), auth.getName());
            }
            new SecurityContextLogoutHandler()
                    .logout(request, response, auth);

            SecurityContextHolder.clearContext();
            return "redirect:/";
        }catch (Exception e){
            return "redirect:/users/settings?error=" + e.getMessage();
        }
    }
}