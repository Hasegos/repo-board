package io.github.repoboard.controller;

import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final UserService userService;

    @GetMapping
    public String showhome(@AuthenticationPrincipal CustomUserPrincipal principal,
                           Model model){
        if(principal != null){
            model.addAttribute("user", principal.getUser());
        }
        return "home";
    }
}