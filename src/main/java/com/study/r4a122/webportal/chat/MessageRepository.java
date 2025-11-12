package com.study.r4a122.webportal.chat;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MessageRepository {

  @Autowired
  private NamedParameterJdbcTemplate jdbc;

  public List<MessageData> findByChannelId(int channelId) {
    final String SQL_SELECT = "SELECT m.id, m.channel_id, m.user_id, u.user_name, m.message_text, " +
        "m.parent_message_id, m.importance, m.is_edited, m.is_deleted, m.created_at, m.updated_at " +
        "FROM message_t m " +
        "LEFT JOIN user_m u ON m.user_id = u.user_id " +
        "WHERE m.channel_id = :channelId AND m.is_deleted = false " +
        "ORDER BY m.created_at ASC";
    Map<String, Object> params = new HashMap<>();
    params.put("channelId", channelId);

    List<Map<String, Object>> resultList = jdbc.queryForList(SQL_SELECT, params);
    List<MessageData> messages = new ArrayList<>();

    for (Map<String, Object> map : resultList) {
      int id = (Integer) map.get("id");
      int channelIdResult = (Integer) map.get("channel_id");
      String userId = (String) map.get("user_id");
      String userName = (String) map.get("user_name");
      String messageText = (String) map.get("message_text");
      Integer parentMessageId = (Integer) map.get("parent_message_id");
      String resultImportance = (String) map.get("importance");
      boolean isEdited = map.get("is_edited") != null && (Boolean) map.get("is_edited");
      boolean isDeleted = map.get("is_deleted") != null && (Boolean) map.get("is_deleted");
      Timestamp createdAt = (Timestamp) map.get("created_at");
      Timestamp updatedAt = (Timestamp) map.get("updated_at");

      LocalDateTime createdAtLocal = createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now();
      LocalDateTime updatedAtLocal = updatedAt != null ? updatedAt.toLocalDateTime() : null;
      MessageData message = new MessageData(id, channelIdResult, userId, userName, messageText, 
          parentMessageId, resultImportance != null ? resultImportance : "NORMAL", isEdited, isDeleted, createdAtLocal, updatedAtLocal);
      messages.add(message);
    }
    return messages;
  }
  
  public MessageData findById(int messageId) {
    final String SQL_SELECT = "SELECT m.id, m.channel_id, m.user_id, u.user_name, m.message_text, " +
        "m.parent_message_id, m.importance, m.is_edited, m.is_deleted, m.created_at, m.updated_at " +
        "FROM message_t m " +
        "LEFT JOIN user_m u ON m.user_id = u.user_id " +
        "WHERE m.id = :messageId";
    Map<String, Object> params = new HashMap<>();
    params.put("messageId", messageId);

    List<Map<String, Object>> resultList = jdbc.queryForList(SQL_SELECT, params);
    if (resultList.isEmpty()) {
      return null;
    }

    Map<String, Object> map = resultList.get(0);
    int id = (Integer) map.get("id");
    int channelId = (Integer) map.get("channel_id");
    String userId = (String) map.get("user_id");
    String userName = (String) map.get("user_name");
    String messageText = (String) map.get("message_text");
    Integer parentMessageId = (Integer) map.get("parent_message_id");
    String resultImportance = (String) map.get("importance");
    boolean isEdited = map.get("is_edited") != null && (Boolean) map.get("is_edited");
    boolean isDeleted = map.get("is_deleted") != null && (Boolean) map.get("is_deleted");
    Timestamp createdAt = (Timestamp) map.get("created_at");
    Timestamp updatedAt = (Timestamp) map.get("updated_at");

    LocalDateTime createdAtLocal = createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now();
    LocalDateTime updatedAtLocal = updatedAt != null ? updatedAt.toLocalDateTime() : null;
    return new MessageData(id, channelId, userId, userName, messageText, 
        parentMessageId, resultImportance != null ? resultImportance : "NORMAL", isEdited, isDeleted, createdAtLocal, updatedAtLocal);
  }
  
  public boolean updateMessage(int messageId, String messageText, String importance, String userId) {
    final String SQL_UPDATE = "UPDATE message_t SET message_text = :messageText, importance = :importance, is_edited = true, updated_at = CURRENT_TIMESTAMP " +
        "WHERE id = :messageId AND user_id = :userId";
    Map<String, Object> params = new HashMap<>();
    params.put("messageId", messageId);
    params.put("messageText", messageText);
    params.put("importance", importance);
    params.put("userId", userId);
    
    return jdbc.update(SQL_UPDATE, params) == 1;
  }
  
  public boolean updateMessageByAdmin(int messageId, String messageText, String importance) {
    final String SQL_UPDATE = "UPDATE message_t SET message_text = :messageText, importance = :importance, is_edited = true, updated_at = CURRENT_TIMESTAMP " +
        "WHERE id = :messageId";
    Map<String, Object> params = new HashMap<>();
    params.put("messageId", messageId);
    params.put("messageText", messageText);
    params.put("importance", importance);
    return jdbc.update(SQL_UPDATE, params) == 1;
  }
  
  public boolean deleteMessage(int messageId, String userId) {
    final String SQL_DELETE = "UPDATE message_t SET is_deleted = true, updated_at = CURRENT_TIMESTAMP " +
        "WHERE id = :messageId AND user_id = :userId";
    Map<String, Object> params = new HashMap<>();
    params.put("messageId", messageId);
    params.put("userId", userId);
    
    return jdbc.update(SQL_DELETE, params) == 1;
  }

  public boolean deleteMessageByAdmin(int messageId) {
    final String SQL_DELETE = "UPDATE message_t SET is_deleted = true, updated_at = CURRENT_TIMESTAMP WHERE id = :messageId";
    Map<String, Object> params = new HashMap<>();
    params.put("messageId", messageId);
    return jdbc.update(SQL_DELETE, params) == 1;
  }

  public int save(MessageData messageData) {
    final String SQL_INSERT = "INSERT INTO message_t(id, channel_id, user_id, message_text, parent_message_id, importance) " +
        "VALUES((SELECT COALESCE(MAX(id), 0) + 1 FROM message_t), :channelId, :userId, :messageText, :parentMessageId, :importance)";
    Map<String, Object> params = new HashMap<>();
    params.put("channelId", messageData.channelId());
    params.put("userId", messageData.userId());
    params.put("messageText", messageData.messageText());
    params.put("parentMessageId", messageData.parentMessageId());
    params.put("importance", messageData.importance());

    return jdbc.update(SQL_INSERT, params);
  }

  public List<MessageData> searchMessages(
      List<Integer> accessibleChannelIds,
      String keyword,
      String userId,
      Integer channelId,
      LocalDateTime dateFrom,
      LocalDateTime dateTo,
      String importanceFilter,
      int limit) {
    if (accessibleChannelIds == null || accessibleChannelIds.isEmpty()) {
      return List.of();
    }

    StringBuilder sql = new StringBuilder(
        "SELECT m.id, m.channel_id, m.user_id, u.user_name, m.message_text, " +
        "m.parent_message_id, m.importance, m.is_edited, m.is_deleted, m.created_at, m.updated_at " +
            "FROM message_t m " +
            "LEFT JOIN user_m u ON m.user_id = u.user_id " +
            "WHERE m.is_deleted = false AND m.channel_id IN (:channelIds)");

    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("channelIds", accessibleChannelIds);

    if (channelId != null) {
      sql.append(" AND m.channel_id = :channelId");
      params.addValue("channelId", channelId);
    }

    if (keyword != null && !keyword.trim().isEmpty()) {
      sql.append(" AND LOWER(m.message_text) LIKE :keyword");
      params.addValue("keyword", "%" + keyword.trim().toLowerCase() + "%");
    }

    if (userId != null && !userId.trim().isEmpty()) {
      sql.append(" AND m.user_id = :userId");
      params.addValue("userId", userId.trim());
    }

    if (dateFrom != null) {
      sql.append(" AND m.created_at >= :dateFrom");
      params.addValue("dateFrom", Timestamp.valueOf(dateFrom));
    }

    if (dateTo != null) {
      sql.append(" AND m.created_at <= :dateTo");
      params.addValue("dateTo", Timestamp.valueOf(dateTo));
    }

    if (importanceFilter != null && !importanceFilter.isBlank() && !"ALL".equalsIgnoreCase(importanceFilter)) {
      sql.append(" AND m.importance = :importance");
      params.addValue("importance", importanceFilter.trim().toUpperCase());
    }

    sql.append(" ORDER BY m.created_at DESC");
    if (limit > 0) {
      sql.append(" LIMIT :limit");
      params.addValue("limit", limit);
    }

    List<Map<String, Object>> resultList = jdbc.queryForList(sql.toString(), params);
    List<MessageData> messages = new ArrayList<>();

    for (Map<String, Object> map : resultList) {
      int id = (Integer) map.get("id");
      int channelIdResult = (Integer) map.get("channel_id");
      String userIdResult = (String) map.get("user_id");
      String userName = (String) map.get("user_name");
      String messageText = (String) map.get("message_text");
      Integer parentMessageId = (Integer) map.get("parent_message_id");
      String mappedImportance = (String) map.get("importance");
      boolean isEdited = map.get("is_edited") != null && (Boolean) map.get("is_edited");
      boolean isDeleted = map.get("is_deleted") != null && (Boolean) map.get("is_deleted");
      Timestamp createdAt = (Timestamp) map.get("created_at");
      Timestamp updatedAt = (Timestamp) map.get("updated_at");

      LocalDateTime createdAtLocal = createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now();
      LocalDateTime updatedAtLocal = updatedAt != null ? updatedAt.toLocalDateTime() : null;
      MessageData message = new MessageData(id, channelIdResult, userIdResult, userName, messageText,
          parentMessageId, mappedImportance != null ? mappedImportance : "NORMAL", isEdited, isDeleted, createdAtLocal, updatedAtLocal);
      messages.add(message);
    }
    return messages;
  }
}

