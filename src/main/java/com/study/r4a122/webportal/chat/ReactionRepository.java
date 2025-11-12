package com.study.r4a122.webportal.chat;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReactionRepository {

  @Autowired
  private NamedParameterJdbcTemplate jdbc;

  public List<ReactionData> findByMessageId(int messageId) {
    final String SQL_SELECT = "SELECT r.id, r.message_id, r.thread_id, r.user_id, u.user_name, r.emoji, r.created_at " +
        "FROM reaction_t r " +
        "LEFT JOIN user_m u ON r.user_id = u.user_id " +
        "WHERE r.message_id = :messageId";
    Map<String, Object> params = new HashMap<>();
    params.put("messageId", messageId);

    return executeReactionQuery(SQL_SELECT, params);
  }

  public List<ReactionData> findByThreadId(int threadId) {
    final String SQL_SELECT = "SELECT r.id, r.message_id, r.thread_id, r.user_id, u.user_name, r.emoji, r.created_at " +
        "FROM reaction_t r " +
        "LEFT JOIN user_m u ON r.user_id = u.user_id " +
        "WHERE r.thread_id = :threadId";
    Map<String, Object> params = new HashMap<>();
    params.put("threadId", threadId);

    return executeReactionQuery(SQL_SELECT, params);
  }

  private List<ReactionData> executeReactionQuery(String sql, Map<String, Object> params) {
    List<Map<String, Object>> resultList = jdbc.queryForList(sql, params);
    List<ReactionData> reactions = new ArrayList<>();

    for (Map<String, Object> map : resultList) {
      int id = (Integer) map.get("id");
      Integer messageId = (Integer) map.get("message_id");
      Integer threadId = (Integer) map.get("thread_id");
      String userId = (String) map.get("user_id");
      String userName = (String) map.get("user_name");
      String emoji = (String) map.get("emoji");
      Timestamp createdAt = (Timestamp) map.get("created_at");

      LocalDateTime createdAtLocal = createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now();
      ReactionData reaction = new ReactionData(id, messageId, threadId, userId, userName, emoji, createdAtLocal);
      reactions.add(reaction);
    }
    return reactions;
  }

  public boolean addReaction(Integer messageId, Integer threadId, String userId, String emoji) {
    // 既存のリアクションをチェック
    final String SQL_CHECK = "SELECT COUNT(*) FROM reaction_t WHERE " +
        (messageId != null ? "message_id = :messageId" : "thread_id = :threadId") +
        " AND user_id = :userId AND emoji = :emoji";
    Map<String, Object> checkParams = new HashMap<>();
    if (messageId != null) {
      checkParams.put("messageId", messageId);
    } else {
      checkParams.put("threadId", threadId);
    }
    checkParams.put("userId", userId);
    checkParams.put("emoji", emoji);

    Integer count = jdbc.queryForObject(SQL_CHECK, checkParams, Integer.class);
    if (count != null && count > 0) {
      return false; // 既に存在する
    }

    final String SQL_INSERT = "INSERT INTO reaction_t(id, message_id, thread_id, user_id, emoji) " +
        "VALUES((SELECT COALESCE(MAX(id), 0) + 1 FROM reaction_t), :messageId, :threadId, :userId, :emoji)";
    Map<String, Object> params = new HashMap<>();
    params.put("messageId", messageId);
    params.put("threadId", threadId);
    params.put("userId", userId);
    params.put("emoji", emoji);

    return jdbc.update(SQL_INSERT, params) == 1;
  }

  public boolean removeReaction(Integer messageId, Integer threadId, String userId, String emoji) {
    final String SQL_DELETE = "DELETE FROM reaction_t WHERE " +
        (messageId != null ? "message_id = :messageId" : "thread_id = :threadId") +
        " AND user_id = :userId AND emoji = :emoji";
    Map<String, Object> params = new HashMap<>();
    if (messageId != null) {
      params.put("messageId", messageId);
    } else {
      params.put("threadId", threadId);
    }
    params.put("userId", userId);
    params.put("emoji", emoji);

    return jdbc.update(SQL_DELETE, params) == 1;
  }
}

