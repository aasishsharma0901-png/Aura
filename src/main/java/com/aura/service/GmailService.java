package com.aura.service;

import com.aura.model.Email;
import com.aura.model.User;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

@Service
public class GmailService {

    private static final Logger log = LoggerFactory.getLogger(GmailService.class);
    private static final String ME = "me";

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    // ── Build Gmail client ────────────────────────────────────────────

    private Gmail buildClient(User user) throws GeneralSecurityException, IOException {
        GoogleCredential credential = new GoogleCredential.Builder()
            .setTransport(GoogleNetHttpTransport.newTrustedTransport())
            .setJsonFactory(JacksonFactory.getDefaultInstance())
            .setClientSecrets(clientId, clientSecret)
            .build()
            .setAccessToken(user.getAccessToken())
            .setRefreshToken(user.getRefreshToken());

        return new Gmail.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JacksonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Aura").build();
    }

    // ── Fetch inbox ───────────────────────────────────────────────────

    public List<Email> fetchInbox(User user, int maxResults) {
        try {
            Gmail gmail = buildClient(user);
            ListMessagesResponse  response = gmail.users().messages()
                .list(ME)
                .setLabelIds(List.of("INBOX"))
                .setMaxResults((long) maxResults)
                .execute();

            List<Message> messages = response.getMessages();
            if (messages == null) return List.of();

            List<Email> emails = new ArrayList<>();
            for (Message msg : messages) {
                Message full = gmail.users().messages()
                    .get(ME, msg.getId())
                    .setFormat("full")
                    .execute();
                emails.add(parseMessage(full));
            }
            return emails;

        } catch (Exception e) {
            log.error("Failed to fetch inbox for user {}", user.getEmail(), e);
            return List.of();
        }
    }

    // ── Fetch a single email ──────────────────────────────────────────

    public Optional<Email> fetchEmail(User user, String messageId) {
        try {
            Gmail gmail = buildClient(user);
            Message msg = gmail.users().messages()
                .get(ME, messageId)
                .setFormat("full")
                .execute();
            return Optional.of(parseMessage(msg));
        } catch (Exception e) {
            log.error("Failed to fetch email {}", messageId, e);
            return Optional.empty();
        }
    }

    // ── Archive (remove INBOX label) ─────────────────────────────────

    public boolean archive(User user, String messageId) {
        return modifyLabels(user, messageId, List.of(), List.of("INBOX"));
    }

    // ── Star ─────────────────────────────────────────────────────────

    public boolean star(User user, String messageId) {
        return modifyLabels(user, messageId, List.of("STARRED"), List.of());
    }

    // ── Trash ─────────────────────────────────────────────────────────

    public boolean trash(User user, String messageId) {
        return modifyLabels(user, messageId, List.of("TRASH"), List.of("INBOX"));
    }

    // ── Mark as read ──────────────────────────────────────────────────

    public boolean markRead(User user, String messageId) {
        return modifyLabels(user, messageId, List.of(), List.of("UNREAD"));
    }

    // ── Send email ────────────────────────────────────────────────────

    public boolean send(User user, String to, String subject, String body, String inReplyToId) {
        try {
            Gmail gmail = buildClient(user);

            String rawEmail = buildRawEmail(user.getEmail(), to, subject, body, inReplyToId);
            Message message = new Message();
            message.setRaw(Base64.getUrlEncoder().encodeToString(rawEmail.getBytes()));

            if (inReplyToId != null) {
                // Get thread ID to reply in the same thread
                Message original = gmail.users().messages().get(ME, inReplyToId).execute();
                message.setThreadId(original.getThreadId());
            }

            gmail.users().messages().send(ME, message).execute();
            return true;
        } catch (Exception e) {
            log.error("Failed to send email", e);
            return false;
        }
    }

    // ── Search ────────────────────────────────────────────────────────

    public List<Email> search(User user, String query, int maxResults) {
        try {
            Gmail gmail = buildClient(user);
            ListMessagesResponse response = gmail.users().messages()
                .list(ME)
                .setQ(query)
                .setMaxResults((long) maxResults)
                .execute();

            List<Message> messages = response.getMessages();
            if (messages == null) return List.of();

            List<Email> emails = new ArrayList<>();
            for (Message msg : messages) {
                Message full = gmail.users().messages()
                    .get(ME, msg.getId())
                    .setFormat("full")
                    .execute();
                emails.add(parseMessage(full));
            }
            return emails;
        } catch (Exception e) {
            log.error("Search failed for query: {}", query, e);
            return List.of();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────

    private boolean modifyLabels(User user, String messageId,
                                  List<String> addLabels, List<String> removeLabels) {
        try {
            Gmail gmail = buildClient(user);
            ModifyMessageRequest req = new ModifyMessageRequest()
                .setAddLabelIds(addLabels)
                .setRemoveLabelIds(removeLabels);
            gmail.users().messages().modify(ME, messageId, req).execute();
            return true;
        } catch (Exception e) {
            log.error("Failed to modify labels on message {}", messageId, e);
            return false;
        }
    }

    private Email parseMessage(Message msg) {
        List<MessagePartHeader> headers = msg.getPayload().getHeaders();
        Map<String, String> headerMap = new HashMap<>();
        if (headers != null) {
            for (MessagePartHeader h : headers) {
                headerMap.put(h.getName().toLowerCase(), h.getValue());
            }
        }

        String from = headerMap.getOrDefault("from", "Unknown");
        String fromName = extractName(from);
        String subject = headerMap.getOrDefault("subject", "(no subject)");
        String date = headerMap.getOrDefault("date", "");
        String unsubscribeHeader = headerMap.get("list-unsubscribe");

        List<String> labels = msg.getLabelIds() != null ? msg.getLabelIds() : List.of();

        return Email.builder()
            .id(msg.getId())
            .threadId(msg.getThreadId())
            .from(from)
            .fromName(fromName)
            .subject(subject)
            .snippet(msg.getSnippet())
            .body(extractBody(msg.getPayload()))
            .date(date)
            .unread(labels.contains("UNREAD"))
            .starred(labels.contains("STARRED"))
            .labels(labels)
            .hasUnsubscribe(unsubscribeHeader != null)
            .unsubscribeUrl(parseUnsubscribeUrl(unsubscribeHeader))
            .build();
    }

    private String extractName(String from) {
        if (from.contains("<")) {
            return from.substring(0, from.indexOf("<")).trim().replace("\"", "");
        }
        return from;
    }

    private String extractBody(MessagePart part) {
        if (part == null) return "";
        if (part.getBody() != null && part.getBody().getData() != null) {
            byte[] decoded = Base64.getUrlDecoder().decode(part.getBody().getData());
            return new String(decoded);
        }
        if (part.getParts() != null) {
            for (MessagePart p : part.getParts()) {
                String body = extractBody(p);
                if (!body.isEmpty()) return body;
            }
        }
        return "";
    }

    private String parseUnsubscribeUrl(String header) {
        if (header == null) return null;
        int start = header.indexOf('<');
        int end = header.indexOf('>');
        if (start >= 0 && end > start) {
            return header.substring(start + 1, end);
        }
        return null;
    }

    private String buildRawEmail(String from, String to, String subject,
                                  String body, String inReplyToId) {
        StringBuilder sb = new StringBuilder();
        sb.append("From: ").append(from).append("\r\n");
        sb.append("To: ").append(to).append("\r\n");
        sb.append("Subject: ").append(subject).append("\r\n");
        if (inReplyToId != null) {
            sb.append("In-Reply-To: ").append(inReplyToId).append("\r\n");
        }
        sb.append("Content-Type: text/plain; charset=utf-8\r\n");
        sb.append("\r\n");
        sb.append(body);
        return sb.toString();
    }
}
