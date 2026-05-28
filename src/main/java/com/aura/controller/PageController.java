package com.aura.controller;

import com.aura.model.User;
import com.aura.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private final UserService userService;

    public PageController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String landing() {
        // Serves static/index.html automatically via Spring's resource handler
        return "redirect:/index.html";
    }

    @GetMapping("/login")
    public String login() {
        return "redirect:/login.html";
    }

    @GetMapping("/inbox")
    public String inbox(@AuthenticationPrincipal OAuth2User oauthUser, Model model) {
        if (oauthUser == null) return "redirect:/login";
        User user = userService.getByEmail(oauthUser.getAttribute("email"));
        model.addAttribute("userName", user.getName());
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("userAvatar", user.getAvatarUrl());
        return "redirect:/inbox.html";
    }

    @GetMapping("/settings")
    public String settings(@AuthenticationPrincipal OAuth2User oauthUser) {
        if (oauthUser == null) return "redirect:/login";
        return "redirect:/settings.html";
    }

    @GetMapping("/pricing")
    public String pricing() {
        return "redirect:/pricing.html";
    }
}
