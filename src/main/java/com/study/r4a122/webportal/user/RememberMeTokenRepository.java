package com.study.r4a122.webportal.user;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RememberMeTokenRepository {

  @Autowired
  private NamedParameterJdbcTemplate jdbc;

  public void revokeTokensByUser(String userId) {
    final String SQL = "UPDATE remember_me_token_t SET revoked = true WHERE user_id = :userId";
    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);
    jdbc.update(SQL, params);
  }

  public RememberMeTokenData save(String userId, String tokenHash, LocalDateTime expiresAt) {
    final String SQL_INSERT = "INSERT INTO remember_me_token_t(id, user_id, token_hash, expires_at) "
        + "VALUES((SELECT COALESCE(MAX(id), 0) + 1 FROM remember_me_token_t), :userId, :tokenHash, :expiresAt)";
    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);
    params.put("tokenHash", tokenHash);
    params.put("expiresAt", expiresAt != null ? Timestamp.valueOf(expiresAt) : null);
    jdbc.update(SQL_INSERT, params);

    final String SQL_SELECT = "SELECT id, user_id, token_hash, issued_at, expires_at, revoked "
        + "FROM remember_me_token_t WHERE user_id = :userId AND token_hash = :tokenHash ORDER BY issued_at DESC";
    List<Map<String, Object>> rows = jdbc.queryForList(SQL_SELECT, params);
    if (rows.isEmpty()) {
      return null;
    }
    Map<String, Object> row = rows.get(0);
    return mapRow(row);
  }

  public RememberMeTokenData findValidToken(String tokenHash) {
    final String SQL_SELECT = "SELECT id, user_id, token_hash, issued_at, expires_at, revoked "
        + "FROM remember_me_token_t WHERE token_hash = :tokenHash AND revoked = false";
    Map<String, Object> params = new HashMap<>();
    params.put("tokenHash", tokenHash);
    List<Map<String, Object>> rows = jdbc.queryForList(SQL_SELECT, params);
    if (rows.isEmpty()) {
      return null;
    }
    return mapRow(rows.get(0));
  }

  public void revokeToken(int id) {
    final String SQL_UPDATE = "UPDATE remember_me_token_t SET revoked = true WHERE id = :id";
    Map<String, Object> params = new HashMap<>();
    params.put("id", id);
    jdbc.update(SQL_UPDATE, params);
  }

  public void cleanupExpired() {
    final String SQL_DELETE = "DELETE FROM remember_me_token_t WHERE (expires_at IS NOT NULL AND expires_at < CURRENT_TIMESTAMP) OR revoked = true";
    jdbc.update(SQL_DELETE, Map.of());
  }

  private RememberMeTokenData mapRow(Map<String, Object> row) {
    Integer id = (Integer) row.get("id");
    String userId = (String) row.get("user_id");
    String tokenHash = (String) row.get("token_hash");
    Timestamp issuedAt = (Timestamp) row.get("issued_at");
    Timestamp expiresAt = (Timestamp) row.get("expires_at");
    boolean revoked = row.get("revoked") != null && (Boolean) row.get("revoked");
    return new RememberMeTokenData(
        id != null ? id : 0,
        userId,
        tokenHash,
        issuedAt != null ? issuedAt.toLocalDateTime() : null,
        expiresAt != null ? expiresAt.toLocalDateTime() : null,
        revoked);
  }
}

