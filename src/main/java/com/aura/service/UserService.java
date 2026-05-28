package com.aura.service;

import com.aura.model.User;
import com.aura.model.UserRepository;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UserService {

    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public User findOrCreate(OAuth2User oauthUser, String accessToken, String refreshToken) {
        String email = oauthUser.getAttribute("email");
        String googleId = oauthUser.getAttribute("sub");

        return userRepo.findByEmail(email).map(u -> {
            // Update tokens on every login
            u.setAccessToken(accessToken);
            if (refreshToken != null) u.setRefreshToken(refreshToken);
            u.setTokenExpiresAt(Instant.now().plusSeconds(3600));
            return userRepo.save(u);
        }).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setName(oauthUser.getAttribute("name"));
            u.setAvatarUrl(oauthUser.getAttribute("picture"));
            u.setGoogleId(googleId);
            u.setAccessToken(accessToken);
            u.setRefreshToken(refreshToken);
            u.setTokenExpiresAt(Instant.now().plusSeconds(3600));
            return userRepo.save(u);
        });
    }

    public User getByEmail(String email) {
        return userRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    public User save(User user) {
        return userRepo.save(user);
    }

    public void deleteByEmail(String email) {
        userRepo.findByEmail(email).ifPresent(userRepo::delete);
    }
}
