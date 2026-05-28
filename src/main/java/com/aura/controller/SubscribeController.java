package com.aura.controller;

import com.aura.model.Subscriber;
import com.aura.model.SubscriberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SubscribeController {

    @Autowired private SubscriberRepository subscriberRepository;
    @Autowired private JavaMailSender mailSender;

    @PostMapping("/subscribe")
    public Map<String, Object> subscribe(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isEmpty())
            return Map.of("success", false, "message", "Email is required.");

        if (subscriberRepository.findByEmail(email).isPresent())
            return Map.of("success", false, "message", "Already subscribed!");

        Subscriber sub = new Subscriber();
        sub.setEmail(email);
        subscriberRepository.save(sub);

        // Send confirmation email
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(email);
            msg.setFrom("aasishsharma0808@gmail.com");
            msg.setSubject("You're subscribed to Aura ✓");
            msg.setText("Hi,\n\nThanks for subscribing to the Aura blog! You'll get the latest updates on AI email features, product news, and tips.\n\nTeam Aura");
            mailSender.send(msg);
        } catch (Exception e) {
            System.out.println("Email send failed: " + e.getMessage());
        }

        return Map.of("success", true);
    }
}