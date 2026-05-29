package com.aura.controller;

import com.aura.model.User;
import com.aura.model.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    @Autowired private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        String email    = body.get("email");
        String password = body.get("password");
        String name     = body.get("name");

        if (email == null || email.isEmpty())
            return Map.of("success", false, "message", "Email is required.");
        if (password == null || password.length() < 8)
            return Map.of("success", false, "message", "Password must be at least 8 characters.");
        if (userRepository.findByEmail(email).isPresent())
            return Map.of("success", false, "message", "Email already registered.");

        User user = new User();
        user.setEmail(email);
        user.setName(name != null && !name.isBlank() ? name : email.split("@")[0]);
        user.setPassword(passwordEncoder.encode(password));
        user.setProvider("local");
        userRepository.save(user);

        // Send welcome email in background — returns instantly
        String finalName = user.getName();
        new Thread(() -> {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(email);
                msg.setFrom(fromEmail);
                msg.setSubject("Welcome to Aura 👋");
                msg.setText(
                    "Hi " + finalName + ",\n\n" +
                    "Welcome to Aura! Your account is ready.\n\n" +
                    "Sign in at https://aura-production-07f3.up.railway.app/login.html\n\n" +
                    "Team Aura"
                );
                mailSender.send(msg);
            } catch (Exception e) {
                System.out.println("Welcome email failed: " + e.getMessage());
            }
        }).start();

        return Map.of("success", true, "message", "Account created! Please log in.");
    }
}
