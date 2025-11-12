package com.study.r4a122.webportal.chat;

import java.time.LocalDateTime;

public record ChannelInvitationData(
    int id,
    int channelId,
    String channelName,
    String inviterId,
    String inviterName,
    String inviteeId,
    String inviteeName,
    String status,
    LocalDateTime createdAt,
    LocalDateTime respondedAt) {
}

