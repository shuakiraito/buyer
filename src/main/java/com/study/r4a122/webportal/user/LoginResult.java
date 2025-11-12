package com.study.r4a122.webportal.user;

public record LoginResult(
    boolean success,
    boolean locked,
    String message) {

  public static LoginResult ok() {
    return new LoginResult(true, false, null);
  }

  public static LoginResult lockedResult(String message) {
    return new LoginResult(false, true, message);
  }

  public static LoginResult error(String message) {
    return new LoginResult(false, false, message);
  }
}

