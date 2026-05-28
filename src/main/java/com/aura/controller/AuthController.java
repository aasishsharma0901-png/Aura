package com.aura.controller;

import com.aura.model.User;
import com.aura.model.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JavaMailSender mailSender;  // ← added here

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        String email    = body.get("email");
        String password = body.get("password");
        String name     = body.get("name");

        if (userRepository.findByEmail(email).isPresent()) {
            return Map.of("success", false, "message", "Email already registered.");
        }

        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(passwordEncoder.encode(password));
        user.setProvider("local");
        userRepository.save(user);

        sendWelcomeEmail(email, name);  // ← called after save

        return Map.of("success", true);
    }

    // ── Welcome email ─────────────────────────────────────────────
    private void sendWelcomeEmail(String toEmail, String name) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setFrom("aasishsharma0909@gmail.com");
        msg.setSubject("Welcome to Aura 👋");
        msg.setText("Hi " + name + ",\n\nWelcome to Aura! Your account has been created successfully.\n\nSign in at https://aura-production-07f3.up.railway.app/login.html\n\nTeam Aura");
        mailSender.send(msg);
    }
}