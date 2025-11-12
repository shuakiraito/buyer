package com.study.r4a122.webportal.chat;

import java.time.LocalDateTime;

public record ThreadData(
    int id,
    int messageId,
    String userId,
    String userName,
    String threadText,
    boolean isEdited,
    boolean isDeleted,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
}

