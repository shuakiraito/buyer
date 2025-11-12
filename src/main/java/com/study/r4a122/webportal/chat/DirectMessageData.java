package com.study.r4a122.webportal.chat;

import java.time.LocalDateTime;

public record DirectMessageData(
    int id,
    String senderId,
    String senderName,
    String receiverId,
    String receiverName,
    String messageText,
    boolean isEdited,
    boolean isDeleted,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
}

