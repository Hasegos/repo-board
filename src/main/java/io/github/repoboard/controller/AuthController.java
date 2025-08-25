package io.github.repoboard.controller;

import io.github.repoboard.dto.UserDTO;
import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/signup")
    public String showRegister(@AuthenticationPrincipal CustomUserPrincipal principal,
                               Model model){
        if(principal != null){
            return "redirect:/";
        }
        model.addAttribute("userDTO", new UserDTO());
        return "auth/signup";
    }

    @PostMapping("/singup")
    public String postRegister(@ModelAttribute("userDTO") @Valid UserDTO userDTO,
                               Model model, BindingResult bindingResult){

        if(bindingResult.hasErrors()){
            return "auth/signup";
        }

        if(userService.findByUsername(userDTO.getUsername()).isPresent()){
            bindingResult.rejectValue("username", "duplicate", "이미 존재하는 회원입니다.");
            return "auth/signup";
        }

        try{
            userService.register(userDTO);
            return "auth/login";
        }catch (DataIntegrityViolationException e){
            bindingResult.rejectValue("username", "duplicate", "이미 존재하는 회원입니다.");
            return "auth/signup";
        }
    }

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