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
public class DirectMessageRepository {

  @Autowired
  private NamedParameterJdbcTemplate jdbc;

  public List<DirectMessageData> findConversation(String userId1, String userId2) {
    final String SQL_SELECT = "SELECT dm.id, dm.sender_id, u1.user_name as sender_name, " +
        "dm.receiver_id, u2.user_name as receiver_name, dm.message_text, " +
        "dm.is_edited, dm.is_deleted, dm.created_at, dm.updated_at " +
        "FROM direct_message_t dm " +
        "LEFT JOIN user_m u1 ON dm.sender_id = u1.user_id " +
        "LEFT JOIN user_m u2 ON dm.receiver_id = u2.user_id " +
        "WHERE ((dm.sender_id = :userId1 AND dm.receiver_id = :userId2) " +
        "OR (dm.sender_id = :userId2 AND dm.receiver_id = :userId1)) " +
        "AND dm.is_deleted = false " +
        "ORDER BY dm.created_at ASC";
    Map<String, Object> params = new HashMap<>();
    params.put("userId1", userId1);
    params.put("userId2", userId2);

    List<Map<String, Object>> resultList = jdbc.queryForList(SQL_SELECT, params);
    List<DirectMessageData> messages = new ArrayList<>();

    for (Map<String, Object> map : resultList) {
      messages.add(mapRow(map));
    }
    return messages;
  }

  public List<String> findConversationPartners(String userId) {
    final String SQL_SELECT = "SELECT DISTINCT " +
        "CASE WHEN sender_id = :userId THEN receiver_id ELSE sender_id END as partner_id " +
        "FROM direct_message_t " +
        "WHERE (sender_id = :userId OR receiver_id = :userId) " +
        "AND is_deleted = false";
    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);

    List<Map<String, Object>> resultList = jdbc.queryForList(SQL_SELECT, params);
    List<String> partners = new ArrayList<>();

    for (Map<String, Object> map : resultList) {
      String partnerId = (String) map.get("partner_id");
      if (partnerId != null) {
        partners.add(partnerId);
      }
    }
    return partners;
  }

  public int save(DirectMessageData messageData) {
    final String SQL_INSERT = "INSERT INTO direct_message_t(id, sender_id, receiver_id, message_text) " +
        "VALUES((SELECT COALESCE(MAX(id), 0) + 1 FROM direct_message_t), :senderId, :receiverId, :messageText)";
    Map<String, Object> params = new HashMap<>();
    params.put("senderId", messageData.senderId());
    params.put("receiverId", messageData.receiverId());
    params.put("messageText", messageData.messageText());

    return jdbc.update(SQL_INSERT, params);
  }

  public boolean updateMessage(int messageId, String messageText, String userId) {
    final String SQL_UPDATE = "UPDATE direct_message_t SET message_text = :messageText, is_edited = true, updated_at = CURRENT_TIMESTAMP "
        + "WHERE id = :messageId AND sender_id = :userId";
    Map<String, Object> params = new HashMap<>();
    params.put("messageId", messageId);
    params.put("messageText", messageText);
    params.put("userId", userId);
    return jdbc.update(SQL_UPDATE, params) == 1;
  }

  public boolean updateMessageByAdmin(int messageId, String messageText) {
    final String SQL_UPDATE = "UPDATE direct_message_t SET message_text = :messageText, is_edited = true, updated_at = CURRENT_TIMESTAMP "
        + "WHERE id = :messageId";
    Map<String, Object> params = new HashMap<>();
    params.put("messageId", messageId);
    params.put("messageText", messageText);
    return jdbc.update(SQL_UPDATE, params) == 1;
  }

  public boolean deleteMessage(int messageId, String userId) {
    final String SQL_DELETE = "UPDATE direct_message_t SET is_deleted = true, updated_at = CURRENT_TIMESTAMP "
        + "WHERE id = :messageId AND (sender_id = :userId OR receiver_id = :userId)";
    Map<String, Object> params = new HashMap<>();
    params.put("messageId", messageId);
    params.put("userId", userId);
    return jdbc.update(SQL_DELETE, params) == 1;
  }

  public boolean deleteMessageByAdmin(int messageId) {
    final String SQL_DELETE = "UPDATE direct_message_t SET is_deleted = true, updated_at = CURRENT_TIMESTAMP "
        + "WHERE id = :messageId";
    Map<String, Object> params = new HashMap<>();
    params.put("messageId", messageId);
    return jdbc.update(SQL_DELETE, params) == 1;
  }

  public DirectMessageData findById(int messageId) {
    final String SQL_SELECT = "SELECT dm.id, dm.sender_id, u1.user_name as sender_name, "
        + "dm.receiver_id, u2.user_name as receiver_name, dm.message_text, "
        + "dm.is_edited, dm.is_deleted, dm.created_at, dm.updated_at "
        + "FROM direct_message_t dm "
        + "LEFT JOIN user_m u1 ON dm.sender_id = u1.user_id "
        + "LEFT JOIN user_m u2 ON dm.receiver_id = u2.user_id "
        + "WHERE dm.id = :messageId";
    Map<String, Object> params = new HashMap<>();
    params.put("messageId", messageId);
    List<Map<String, Object>> resultList = jdbc.queryForList(SQL_SELECT, params);
    if (resultList.isEmpty()) {
      return null;
    }
    return mapRow(resultList.get(0));
  }

  private DirectMessageData mapRow(Map<String, Object> map) {
    int id = (Integer) map.get("id");
    String senderId = (String) map.get("sender_id");
    String senderName = (String) map.get("sender_name");
    String receiverId = (String) map.get("receiver_id");
    String receiverName = (String) map.get("receiver_name");
    String messageText = (String) map.get("message_text");
    boolean isEdited = map.get("is_edited") != null && (Boolean) map.get("is_edited");
    boolean isDeleted = map.get("is_deleted") != null && (Boolean) map.get("is_deleted");
    Timestamp createdAt = (Timestamp) map.get("created_at");
    Timestamp updatedAt = (Timestamp) map.get("updated_at");

    LocalDateTime createdAtLocal = createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now();
    LocalDateTime updatedAtLocal = updatedAt != null ? updatedAt.toLocalDateTime() : null;
    return new DirectMessageData(
        id,
        senderId,
        senderName,
        receiverId,
        receiverName,
        messageText,
        isEdited,
        isDeleted,
        createdAtLocal,
        updatedAtLocal);
  }
}

