package io.github.repoboard.controller;

import io.github.repoboard.security.core.CustomUserPrincipal;
import io.github.repoboard.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    public String showAdmin(@AuthenticationPrincipal CustomUserPrincipal principal,
                            @RequestParam(value = "userId", required = false) Long userId,
                            Model model){

        model.addAttribute("user",principal.getUser());
        model.addAttribute("users",adminService.getAllUsers());

        if (userId != null) {
            model.addAttribute("selectedUser", adminService.getUserById(userId));
        }

        return "admin/admin";
    }

    @GetMapping("/logs")
    @ResponseBody
    public List<String> getAdminLogs(@RequestParam(defaultValue = "0") int page){
        int linesPerPage = 50;
        return adminService.readAdminLogs(page, linesPerPage);
    }

    @PostMapping("/delete/{userId}")
    public String deleteUser(@PathVariable Long userId){
        adminService.deleteUser(userId);
        return "redirect:/admin";
    }

    @PostMapping("/restore/{username}")
    public String  restoreUser(@PathVariable String username){
        adminService.restoreDeletedUser(username);
        return "redirect:/admin/deleted";
    }

    @PostMapping("/toggle/{userId}")
    public String toggleUserStatus(@PathVariable Long userId){
        adminService.toggleUserStatus(userId);
        return "redirect:/admin";
    }
}