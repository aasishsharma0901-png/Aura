package com.aura.controller;

import com.aura.model.Subscriber;
import com.aura.model.SubscriberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SubscribeController {

    @Autowired private SubscriberRepository subscriberRepository;
    @Autowired private JavaMailSender mailSender;

    // Auto-reads from application.properties — always matches SMTP login
    @Value("${spring.mail.username}")
    private String fromEmail;

    @PostMapping("/subscribe")
    public Map<String, Object> subscribe(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isEmpty())
            return Map.of("success", false, "message", "Email is required.");

        if (subscriberRepository.findByEmail(email).isPresent())
            return Map.of("success", false, "message", "Already subscribed!");

        // Save to DB instantly
        Subscriber sub = new Subscriber();
        sub.setEmail(email);
        subscriberRepository.save(sub);

        // Send confirmation email in background — response returns immediately
        String toEmail = email;
        new Thread(() -> {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(toEmail);
                msg.setFrom(fromEmail);
                msg.setSubject("You're subscribed to Aura ✓");
                msg.setText(
                    "Hi!\n\n" +
                    "Thanks for subscribing to the Aura blog. " +
                    "You'll get the latest updates on AI email features, product news, and tips.\n\n" +
                    "Team Aura"
                );
                mailSender.send(msg);
            } catch (Exception e) {
                System.out.println("Subscribe email failed: " + e.getMessage());
            }

            // Also notify yourself of new subscriber
            try {
                SimpleMailMessage notify = new SimpleMailMessage();
                notify.setTo(fromEmail);
                notify.setFrom(fromEmail);
                notify.setSubject("New subscriber — " + toEmail);
                notify.setText("New blog subscriber: " + toEmail);
                mailSender.send(notify);
            } catch (Exception e) {
                System.out.println("Subscriber notify failed: " + e.getMessage());
            }
        }).start();

        return Map.of("success", true);
    }
}
