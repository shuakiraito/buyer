package com.study.r4a122.webportal.user;

import java.time.LocalDateTime;

public record RememberMeTokenData(
    int id,
    String userId,
    String tokenHash,
    LocalDateTime issuedAt,
    LocalDateTime expiresAt,
    boolean revoked) {
}

