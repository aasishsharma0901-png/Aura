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
        String email = body.get("email");
        String name  = body.get("name");
        String role  = body.get("role");

        if (email == null || name == null || role == null)
            return Map.of("success", false, "message", "Please fill in all required fields.");

        // Save to database
        JobApplication app = new JobApplication();
        app.setName(name);
        app.setEmail(email);
        app.setRole(role);
        app.setLinkedin(body.get("linkedin"));
        app.setMessage(body.get("message"));
        applicationRepository.save(app);

        // Email to applicant
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(email);
            msg.setFrom("aasishsharma0808@gmail.com");
            msg.setSubject("We received your application — Aura");
            msg.setText("Hi " + name + ",\n\nThanks for applying for the " + role + " role at Aura! We've received your application and will review it shortly.\n\nIf you're a good fit we'll reach out within 5-7 business days.\n\nTeam Aura");
            mailSender.send(msg);
        } catch (Exception e) {
            System.out.println("Email send failed: " + e.getMessage());
        }

        // Email to you (notification)
        try {
            SimpleMailMessage notify = new SimpleMailMessage();
            notify.setTo("aasishsharma0808@gmail.com");
            notify.setFrom("aasishsharma0808@gmail.com");
            notify.setSubject("New job application — " + role);
            notify.setText("New application received!\n\nName: " + name + "\nEmail: " + email + "\nRole: " + role + "\nLinkedIn: " + body.get("linkedin") + "\n\nMessage:\n" + body.get("message"));
            mailSender.send(notify);
        } catch (Exception e) {
            System.out.println("Notify email failed: " + e.getMessage());
        }

        return Map.of("success", true);
    }
}