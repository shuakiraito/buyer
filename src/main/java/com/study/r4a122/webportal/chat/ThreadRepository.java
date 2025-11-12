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
public class ThreadRepository {

  @Autowired
  private NamedParameterJdbcTemplate jdbc;

  public List<ThreadData> findByMessageId(int messageId) {
    final String SQL_SELECT = "SELECT t.id, t.message_id, t.user_id, u.user_name, t.thread_text, " +
        "t.is_edited, t.is_deleted, t.created_at, t.updated_at " +
        "FROM thread_t t " +
        "LEFT JOIN user_m u ON t.user_id = u.user_id " +
        "WHERE t.message_id = :messageId AND t.is_deleted = false " +
        "ORDER BY t.created_at ASC";
    Map<String, Object> params = new HashMap<>();
    params.put("messageId", messageId);

    List<Map<String, Object>> resultList = jdbc.queryForList(SQL_SELECT, params);
    List<ThreadData> threads = new ArrayList<>();

    for (Map<String, Object> map : resultList) {
      int id = (Integer) map.get("id");
      int msgId = (Integer) map.get("message_id");
      String userId = (String) map.get("user_id");
      String userName = (String) map.get("user_name");
      String threadText = (String) map.get("thread_text");
      boolean isEdited = map.get("is_edited") != null && (Boolean) map.get("is_edited");
      boolean isDeleted = map.get("is_deleted") != null && (Boolean) map.get("is_deleted");
      Timestamp createdAt = (Timestamp) map.get("created_at");
      Timestamp updatedAt = (Timestamp) map.get("updated_at");

      LocalDateTime createdAtLocal = createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now();
      LocalDateTime updatedAtLocal = updatedAt != null ? updatedAt.toLocalDateTime() : null;
      ThreadData thread = new ThreadData(id, msgId, userId, userName, threadText, isEdited, isDeleted, createdAtLocal, updatedAtLocal);
      threads.add(thread);
    }
    return threads;
  }

  public ThreadData findById(int threadId) {
    final String SQL_SELECT = "SELECT t.id, t.message_id, t.user_id, u.user_name, t.thread_text, " +
        "t.is_edited, t.is_deleted, t.created_at, t.updated_at " +
        "FROM thread_t t " +
        "LEFT JOIN user_m u ON t.user_id = u.user_id " +
        "WHERE t.id = :threadId";
    Map<String, Object> params = new HashMap<>();
    params.put("threadId", threadId);

    List<Map<String, Object>> resultList = jdbc.queryForList(SQL_SELECT, params);
    if (resultList.isEmpty()) {
      return null;
    }

    Map<String, Object> map = resultList.get(0);
    int id = (Integer) map.get("id");
    int msgId = (Integer) map.get("message_id");
    String userId = (String) map.get("user_id");
    String userName = (String) map.get("user_name");
    String threadText = (String) map.get("thread_text");
    boolean isEdited = map.get("is_edited") != null && (Boolean) map.get("is_edited");
    boolean isDeleted = map.get("is_deleted") != null && (Boolean) map.get("is_deleted");
    Timestamp createdAt = (Timestamp) map.get("created_at");
    Timestamp updatedAt = (Timestamp) map.get("updated_at");

    LocalDateTime createdAtLocal = createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now();
    LocalDateTime updatedAtLocal = updatedAt != null ? updatedAt.toLocalDateTime() : null;
    return new ThreadData(id, msgId, userId, userName, threadText, isEdited, isDeleted, createdAtLocal, updatedAtLocal);
  }

  public int save(ThreadData threadData) {
    final String SQL_INSERT = "INSERT INTO thread_t(id, message_id, user_id, thread_text) " +
        "VALUES((SELECT COALESCE(MAX(id), 0) + 1 FROM thread_t), :messageId, :userId, :threadText)";
    Map<String, Object> params = new HashMap<>();
    params.put("messageId", threadData.messageId());
    params.put("userId", threadData.userId());
    params.put("threadText", threadData.threadText());

    return jdbc.update(SQL_INSERT, params);
  }

  public boolean updateThread(int threadId, String threadText, String userId) {
    final String SQL_UPDATE = "UPDATE thread_t SET thread_text = :threadText, is_edited = true, updated_at = CURRENT_TIMESTAMP " +
        "WHERE id = :threadId AND user_id = :userId";
    Map<String, Object> params = new HashMap<>();
    params.put("threadId", threadId);
    params.put("threadText", threadText);
    params.put("userId", userId);
    
    return jdbc.update(SQL_UPDATE, params) == 1;
  }

  public boolean updateThreadByAdmin(int threadId, String threadText) {
    final String SQL_UPDATE = "UPDATE thread_t SET thread_text = :threadText, is_edited = true, updated_at = CURRENT_TIMESTAMP " +
        "WHERE id = :threadId";
    Map<String, Object> params = new HashMap<>();
    params.put("threadId", threadId);
    params.put("threadText", threadText);
    return jdbc.update(SQL_UPDATE, params) == 1;
  }

  public boolean deleteThread(int threadId, String userId) {
    final String SQL_DELETE = "UPDATE thread_t SET is_deleted = true, updated_at = CURRENT_TIMESTAMP " +
        "WHERE id = :threadId AND user_id = :userId";
    Map<String, Object> params = new HashMap<>();
    params.put("threadId", threadId);
    params.put("userId", userId);
    
    return jdbc.update(SQL_DELETE, params) == 1;
  }

  public boolean deleteThreadByAdmin(int threadId) {
    final String SQL_DELETE = "UPDATE thread_t SET is_deleted = true, updated_at = CURRENT_TIMESTAMP WHERE id = :threadId";
    Map<String, Object> params = new HashMap<>();
    params.put("threadId", threadId);
    return jdbc.update(SQL_DELETE, params) == 1;
  }
}

