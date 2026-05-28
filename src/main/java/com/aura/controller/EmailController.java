package com.aura.controller;

import com.aura.model.Email;
import com.aura.model.User;
import com.aura.service.AiService;
import com.aura.service.GmailService;
import com.aura.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private final GmailService gmailService;
    private final AiService aiService;
    private final UserService userService;

    public EmailController(GmailService gmailService, AiService aiService,
                           UserService userService) {
        this.gmailService = gmailService;
        this.aiService = aiService;
        this.userService = userService;
    }

    // GET /api/emails?max=50
    @GetMapping
    public ResponseEntity<List<Email>> listInbox(
        @AuthenticationPrincipal OAuth2User oauth,
        @RequestParam(defaultValue = "50") int max
    ) {
        User user = getUser(oauth);
        List<Email> emails = gmailService.fetchInbox(user, max);
        return ResponseEntity.ok(emails);
    }

    // GET /api/emails/search?q=from:linear
    @GetMapping("/search")
    public ResponseEntity<List<Email>> search(
        @AuthenticationPrincipal OAuth2User oauth,
        @RequestParam String q,
        @RequestParam(defaultValue = "20") int max
    ) {
        User user = getUser(oauth);
        return ResponseEntity.ok(gmailService.search(user, q, max));
    }

    // GET /api/emails/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Email> getEmail(
        @AuthenticationPrincipal OAuth2User oauth,
        @PathVariable String id
    ) {
        User user = getUser(oauth);
        Optional<Email> email = gmailService.fetchEmail(user, id);
        return email.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/emails/{id}/summarize
    @PostMapping("/{id}/summarize")
    public ResponseEntity<Map<String, String>> summarize(
        @AuthenticationPrincipal OAuth2User oauth,
        @PathVariable String id
    ) {
        User user = getUser(oauth);
        Optional<Email> email = gmailService.fetchEmail(user, id);
        if (email.isEmpty()) return ResponseEntity.notFound().build();

        Email e = email.get();
        String summary = aiService.summarize(e.getSubject(), e.getBody());
        String category = aiService.triage(e.getFromName(), e.getSubject(), e.getSnippet());
        return ResponseEntity.ok(Map.of(
            "summary", summary != null ? summary : "",
            "category", category
        ));
    }

    // POST /api/emails/{id}/smart-replies
    @PostMapping("/{id}/smart-replies")
    public ResponseEntity<Map<String, List<String>>> smartReplies(
        @AuthenticationPrincipal OAuth2User oauth,
        @PathVariable String id
    ) {
        User user = getUser(oauth);
        Optional<Email> email = gmailService.fetchEmail(user, id);
        if (email.isEmpty()) return ResponseEntity.notFound().build();

        Email e = email.get();
        List<String> replies = aiService.smartReplies(e.getSubject(), e.getBody());
        return ResponseEntity.ok(Map.of("replies", replies));
    }

    // POST /api/emails/{id}/archive
    @PostMapping("/{id}/archive")
    public ResponseEntity<Map<String, Boolean>> archive(
        @AuthenticationPrincipal OAuth2User oauth,
        @PathVariable String id
    ) {
        User user = getUser(oauth);
        boolean ok = gmailService.archive(user, id);
        return ResponseEntity.ok(Map.of("success", ok));
    }

    // POST /api/emails/{id}/star
    @PostMapping("/{id}/star")
    public ResponseEntity<Map<String, Boolean>> star(
        @AuthenticationPrincipal OAuth2User oauth,
        @PathVariable String id
    ) {
        User user = getUser(oauth);
        boolean ok = gmailService.star(user, id);
        return ResponseEntity.ok(Map.of("success", ok));
    }

    // POST /api/emails/{id}/trash
    @PostMapping("/{id}/trash")
    public ResponseEntity<Map<String, Boolean>> trash(
        @AuthenticationPrincipal OAuth2User oauth,
        @PathVariable String id
    ) {
        User user = getUser(oauth);
        boolean ok = gmailService.trash(user, id);
        return ResponseEntity.ok(Map.of("success", ok));
    }

    // POST /api/emails/send
    @PostMapping("/send")
    public ResponseEntity<Map<String, Boolean>> send(
        @AuthenticationPrincipal OAuth2User oauth,
        @RequestBody SendRequest req
    ) {
        User user = getUser(oauth);
        boolean ok = gmailService.send(user, req.to(), req.subject(), req.body(), req.inReplyToId());
        return ResponseEntity.ok(Map.of("success", ok));
    }

    // POST /api/emails/compose-ai  { instruction, context }
    @PostMapping("/compose-ai")
    public ResponseEntity<Map<String, String>> composeAi(
        @AuthenticationPrincipal OAuth2User oauth,
        @RequestBody ComposeRequest req
    ) {
        String draft = aiService.compose(req.instruction(), req.context());
        return ResponseEntity.ok(Map.of("draft", draft != null ? draft : ""));
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private User getUser(OAuth2User oauth) {
        return userService.getByEmail(oauth.getAttribute("email"));
    }

    record SendRequest(String to, String subject, String body, String inReplyToId) {}
    record ComposeRequest(String instruction, String context) {}
}
