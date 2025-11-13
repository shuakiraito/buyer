package com.study.r4a122.webportal.chat;

import java.security.Principal;

/**
 * WebSocket認証用のPrincipal実装クラス
 */
public class UserPrincipal implements Principal {
  private final String userId;

  public UserPrincipal(String userId) {
    this.userId = userId;
  }

  @Override
  public String getName() {
    return userId;
  }
}

