package com.study.r4a122.webportal.chat;

import java.time.LocalDateTime;

public record ActivityData(
    int id,
    String userId,
    String userName,
    String activityType,
    String activityMessage,
    Integer relatedId,
    String relatedType,
    LocalDateTime createdAt) {
}

