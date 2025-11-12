package com.study.r4a122.webportal.user;

import java.time.LocalDateTime;

public record UserData(
    String userId,
    String password,
    String userName,
    String role,
    boolean enabled,
    boolean locked,
    int failedAttempts,
    LocalDateTime lockedAt,
    LocalDateTime lastLoginAt) {

  public UserData(String userId, String password, String userName, String role, boolean enabled) {
    this(userId, password, userName, role, enabled, false, 0, null, null);
  }
}
