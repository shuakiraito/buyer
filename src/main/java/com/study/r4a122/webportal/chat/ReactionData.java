package com.study.r4a122.webportal.chat;

import java.time.LocalDateTime;

public record ReactionData(
    int id,
    Integer messageId,
    Integer threadId,
    String userId,
    String userName,
    String emoji,
    LocalDateTime createdAt) {
}

