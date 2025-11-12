package com.study.r4a122.webportal.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class RememberMeService {

  private static final String COOKIE_NAME = "YAMABIKO_REMEMBER_ME";
  private static final int EXPIRY_DAYS = 30;

  @Autowired
  private RememberMeTokenRepository tokenRepository;

  @Autowired
  private UserRepository userRepository;

  public void issueToken(String userId, HttpServletResponse response) {
    tokenRepository.revokeTokensByUser(userId);
    tokenRepository.cleanupExpired();

    String rawToken = UUID.randomUUID() + ":" + UUID.randomUUID();
    String tokenHash = hash(rawToken);
    LocalDateTime expiresAt = LocalDateTime.now().plusDays(EXPIRY_DAYS);
    tokenRepository.save(userId, tokenHash, expiresAt);

    String cookieValue = Base64.getEncoder().encodeToString((userId + ":" + rawToken).getBytes(StandardCharsets.UTF_8));
    Cookie cookie = new Cookie(COOKIE_NAME, cookieValue);
    cookie.setHttpOnly(true);
    cookie.setPath("/");
    cookie.setMaxAge(EXPIRY_DAYS * 24 * 60 * 60);
    response.addCookie(cookie);
  }

  public void clearToken(String userId, HttpServletResponse response) {
    tokenRepository.revokeTokensByUser(userId);
    Cookie cookie = new Cookie(COOKIE_NAME, "");
    cookie.setPath("/");
    cookie.setMaxAge(0);
    cookie.setHttpOnly(true);
    response.addCookie(cookie);
  }

  public UserData autoLogin(HttpServletRequest request, HttpServletResponse response) {
    Cookie cookie = getRememberMeCookie(request);
    if (cookie == null || cookie.getValue() == null || cookie.getValue().isEmpty()) {
      return null;
    }

    try {
      String decoded = new String(Base64.getDecoder().decode(cookie.getValue()), StandardCharsets.UTF_8);
      String[] parts = decoded.split(":", 2);
      if (parts.length != 2) {
        clearCookie(response);
        return null;
      }
      String userId = parts[0];
      String rawToken = parts[1];
      String tokenHash = hash(rawToken);

      RememberMeTokenData tokenData = tokenRepository.findValidToken(tokenHash);
      if (tokenData == null
          || tokenData.expiresAt() != null && tokenData.expiresAt().isBefore(LocalDateTime.now())
          || !tokenData.userId().equals(userId)) {
        if (tokenData != null) {
          tokenRepository.revokeToken(tokenData.id());
        }
        clearCookie(response);
        return null;
      }

      UserData userData = userRepository.findByUserId(userId);
      if (userData == null || userData.locked() || !userData.enabled()) {
        tokenRepository.revokeToken(tokenData.id());
        clearCookie(response);
        return null;
      }

      return userData;
    } catch (IllegalArgumentException ex) {
      clearCookie(response);
      return null;
    }
  }

  private Cookie getRememberMeCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (COOKIE_NAME.equals(cookie.getName())) {
        return cookie;
      }
    }
    return null;
  }

  private void clearCookie(HttpServletResponse response) {
    Cookie cookie = new Cookie(COOKIE_NAME, "");
    cookie.setPath("/");
    cookie.setMaxAge(0);
    cookie.setHttpOnly(true);
    response.addCookie(cookie);
  }

  private String hash(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hashed);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}

