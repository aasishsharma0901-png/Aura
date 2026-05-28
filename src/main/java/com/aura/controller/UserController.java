package com.aura.controller;

import com.aura.model.User;
import com.aura.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    private User getUser(OAuth2User oauth, Authentication auth) {
        if (oauth != null) {
            return userService.getByEmail(oauth.getAttribute("email"));
        }
        if (auth != null) {
            return userService.getByEmail(auth.getName());
        }
        return null;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(
        @AuthenticationPrincipal OAuth2User oauth,
        Authentication auth
    ) {
        User user = getUser(oauth, auth);
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of(
            "id",        user.getId(),
            "email",     user.getEmail(),
            "name",      user.getName() != null ? user.getName() : "",
            "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
            "plan",      user.getPlan()
        ));
    }

    @PatchMapping("/me")
    public ResponseEntity<Map<String, String>> updateMe(
        @AuthenticationPrincipal OAuth2User oauth,
        Authentication auth,
        @RequestBody Map<String, String> body
    ) {
        User user = getUser(oauth, auth);
        if (user == null) return ResponseEntity.status(401).build();
        if (body.containsKey("name")) user.setName(body.get("name"));
        userService.save(user);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteAccount(
        @AuthenticationPrincipal OAuth2User oauth,
        Authentication auth
    ) {
        User user = getUser(oauth, auth);
        if (user == null) return ResponseEntity.status(401).build();
        userService.deleteByEmail(user.getEmail());
        return ResponseEntity.noContent().build();
    }
}