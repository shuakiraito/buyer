package com.study.r4a122.webportal.chat;

import java.time.LocalDateTime;

public record ChannelRequestData(
    int id,
    String channelName,
    String description,
    boolean isPublic,
    String requestedBy,
    String requestedByName,
    String status,
    LocalDateTime createdAt,
    String reviewedBy,
    LocalDateTime reviewedAt) {
}

