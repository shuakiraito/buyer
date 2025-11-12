package com.study.r4a122.webportal.chat;

import java.time.LocalDateTime;

public record MessageReadData(
    int id,
    int messageId,
    String userId,
    String userName,
    LocalDateTime readAt) {
}

