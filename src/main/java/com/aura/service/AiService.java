package com.aura.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

  private static final String GEMINI_URL =
    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    @Value("${gemini.api.key}")
    private String apiKey;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // ── Summarize ─────────────────────────────────────────────────────
    public String summarize(String subject, String body) {
        String prompt = """
            Summarize this email in 1-2 short sentences. Be concise and direct.

            Subject: %s
            Body: %s
            """.formatted(subject, truncate(body, 2000));

        return callGemini(prompt);
    }

    // ── Triage ────────────────────────────────────────────────────────
    public String triage(String from, String subject, String snippet) {
        String prompt = """
            Classify this email into exactly ONE category.
            Reply with only the single category word, nothing else.

            Categories:
            - Priority   (needs my action today)
            - Follow-up  (I need to check back on this)
            - Update     (FYI notification, no action needed)
            - Archive    (newsletter, promotion, receipt)

            From: %s
            Subject: %s
            Snippet: %s

            Category:""".formatted(from, subject, snippet);

        String result = callGemini(prompt);
        if (result == null) return "Update";
        for (String cat : List.of("Priority", "Follow-up", "Update", "Archive")) {
            if (result.contains(cat)) return cat;
        }
        return "Update";
    }

    // ── Smart replies ─────────────────────────────────────────────────
    public List<String> smartReplies(String subject, String body) {
        String prompt = """
            Generate exactly 3 short email reply options.
            Return them as a numbered list (1. 2. 3.), one per line, nothing else.
            Each reply should be under 10 words.

            Subject: %s
            Body: %s
            """.formatted(subject, truncate(body, 1000));

        String result = callGemini(prompt);
        if (result == null || result.isBlank()) {
            return List.of("Thanks!", "Got it.", "Will follow up.");
        }
        return result.lines()
            .map(l -> l.replaceAll("^\\d+\\.\\s*", "").trim())
            .filter(l -> !l.isBlank())
            .limit(3)
            .toList();
    }

    // ── AI Compose ────────────────────────────────────────────────────
    public String compose(String instruction, String context) {
        String prompt = """
            Write a professional email based on the instruction below.
            Return only the email body — no subject line, no preamble.

            Context: %s
            Instruction: %s
            """.formatted(context != null ? context : "none", instruction);

        return callGemini(prompt);
    }

    // ── Call Gemini API ───────────────────────────────────────────────
    private String callGemini(String prompt) {
        try {
            String requestBody = mapper.writeValueAsString(
                new GeminiRequest(
                    List.of(new Content(
                        List.of(new Part(prompt))
                    ))
                )
            );

            Request request = new Request.Builder()
                .url(GEMINI_URL + apiKey)
                .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                .build();

           try (Response response = client.newCall(request).execute()) {
    String body = response.body() != null ? response.body().string() : "";
    // Print full response so we can see what's happening
    if (!response.isSuccessful()) {
        log.error("Gemini API error {}: {}", response.code(), body);
        return null;
    }
    JsonNode json = mapper.readTree(body);
    String text = json.at("/candidates/0/content/parts/0/text").asText(null);
    System.out.println("=== GEMINI PARSED TEXT: " + text);
    return text;
}
        } catch (Exception e) {
            log.error("Gemini API call failed", e);
            return null;
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    // ── Records for JSON serialization ────────────────────────────────
    record GeminiRequest(List<Content> contents) {}
    record Content(List<Part> parts) {}
    record Part(String text) {}
}