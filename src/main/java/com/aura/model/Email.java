package com.aura.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Email {

    private String id;
    private String threadId;
    private String from;
    private String fromName;
    private String subject;
    private String snippet;
    private String body;           // full HTML/plain body
    private String date;
    private boolean unread;
    private boolean starred;
    private List<String> labels;

    // Populated by AI service
    private String aiSummary;
    private String triageCategory;  // Priority | Follow-up | Update | Archive
    private List<String> smartReplies;
    private boolean hasUnsubscribe;
    private String unsubscribeUrl;
}
