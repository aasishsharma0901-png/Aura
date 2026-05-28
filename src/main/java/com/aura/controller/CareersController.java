package com.aura.controller;

import com.aura.model.JobApplication;
import com.aura.model.JobApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CareersController {

    @Autowired private JobApplicationRepository applicationRepository;
    @Autowired private JavaMailSender mailSender;

    @PostMapping("/apply")
    public Map<String, Object> apply(@RequestBody Map<String, String> body) {
        String email   = body.get("email");
        String name    = body.get("name");
        String role    = body.get("role");
        String linkedin = body.get("linkedin");
        String message  = body.get("message");

        if (email == null || email.isBlank() || name == null || name.isBlank() || role == null || role.isBlank())
            return Map.of("success", false, "message", "Please fill in all required fields.");

        // Save to DB immediately — this is fast
        JobApplication app = new JobApplication();
        app.setName(name);
        app.setEmail(email);
        app.setRole(role);
        app.setLinkedin(linkedin);
        app.setMessage(message);
        applicationRepository.save(app);

        // Send both emails in the background so response returns instantly
        new Thread(() -> {
            // Confirmation email to applicant
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(email);
                msg.setFrom("aasishsharma0808@gmail.com");
                msg.setSubject("We received your application — Aura");
                msg.setText(
                    "Hi " + name + ",\n\n" +
                    "Thanks for applying for the " + role + " role at Aura!\n\n" +
                    "We've received your application and will review it shortly. " +
                    "If you're a good fit we'll reach out within 5–7 business days.\n\n" +
                    "Team Aura"
                );
                mailSender.send(msg);
            } catch (Exception e) {
                System.out.println("Applicant email failed: " + e.getMessage());
            }

            // Notification email to you
            try {
                SimpleMailMessage notify = new SimpleMailMessage();
                notify.setTo("aasishsharma0808@gmail.com");
                notify.setFrom("aasishsharma0808@gmail.com");
                notify.setSubject("New job application — " + role);
                notify.setText(
                    "New application received!\n\n" +
                    "Name:     " + name + "\n" +
                    "Email:    " + email + "\n" +
                    "Role:     " + role + "\n" +
                    "LinkedIn: " + (linkedin != null ? linkedin : "—") + "\n\n" +
                    "Message:\n" + (message != null ? message : "—")
                );
                mailSender.send(notify);
            } catch (Exception e) {
                System.out.println("Notify email failed: " + e.getMessage());
            }
        }).start();

        // Return success immediately — don't wait for emails
        return Map.of("success", true);
    }
}
