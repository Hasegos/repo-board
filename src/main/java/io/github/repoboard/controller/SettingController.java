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

@Controller
@RequiredArgsConstructor
@RequestMapping("/users/settings")
public class SettingController {

    private final SettingService settingService;

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