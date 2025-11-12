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
public class MessageReadRepository {

  @Autowired
  private NamedParameterJdbcTemplate jdbc;

  public void markAsRead(int messageId, String userId) {
    final String SQL_CHECK = "SELECT COUNT(*) FROM message_read_t WHERE message_id = :messageId AND user_id = :userId";
    Map<String, Object> checkParams = new HashMap<>();
    checkParams.put("messageId", messageId);
    checkParams.put("userId", userId);

    Integer count = jdbc.queryForObject(SQL_CHECK, checkParams, Integer.class);
    if (count != null && count > 0) {
      return; // 既に読まれている
    }

    final String SQL_INSERT = "INSERT INTO message_read_t(id, message_id, user_id) " +
        "VALUES((SELECT COALESCE(MAX(id), 0) + 1 FROM message_read_t), :messageId, :userId)";
    Map<String, Object> params = new HashMap<>();
    params.put("messageId", messageId);
    params.put("userId", userId);

    jdbc.update(SQL_INSERT, params);
  }

  public List<MessageReadData> findUnreadUsers(int messageId) {
    // チャンネルの全メンバーを取得し、未読者を特定
    final String SQL_SELECT = "SELECT u.user_id, u.user_name " +
        "FROM channel_member_t cm " +
        "JOIN user_m u ON cm.user_id = u.user_id " +
        "WHERE cm.channel_id = (SELECT channel_id FROM message_t WHERE id = :messageId) " +
        "AND u.user_id NOT IN (SELECT user_id FROM message_read_t WHERE message_id = :messageId) " +
        "AND u.role = 'ROLE_STUDENT'";
    Map<String, Object> params = new HashMap<>();
    params.put("messageId", messageId);

    List<Map<String, Object>> resultList = jdbc.queryForList(SQL_SELECT, params);
    List<MessageReadData> unreadUsers = new ArrayList<>();

    for (Map<String, Object> map : resultList) {
      String userId = (String) map.get("user_id");
      String userName = (String) map.get("user_name");
      MessageReadData readData = new MessageReadData(0, messageId, userId, userName, null);
      unreadUsers.add(readData);
    }
    return unreadUsers;
  }

  public List<MessageReadData> findByMessageId(int messageId) {
    final String SQL_SELECT = "SELECT mr.id, mr.message_id, mr.user_id, u.user_name, mr.read_at " +
        "FROM message_read_t mr " +
        "LEFT JOIN user_m u ON mr.user_id = u.user_id " +
        "WHERE mr.message_id = :messageId";
    Map<String, Object> params = new HashMap<>();
    params.put("messageId", messageId);

    List<Map<String, Object>> resultList = jdbc.queryForList(SQL_SELECT, params);
    List<MessageReadData> readDataList = new ArrayList<>();

    for (Map<String, Object> map : resultList) {
      int id = (Integer) map.get("id");
      int msgId = (Integer) map.get("message_id");
      String userId = (String) map.get("user_id");
      String userName = (String) map.get("user_name");
      Timestamp readAt = (Timestamp) map.get("read_at");

      LocalDateTime readAtLocal = readAt != null ? readAt.toLocalDateTime() : LocalDateTime.now();
      MessageReadData readData = new MessageReadData(id, msgId, userId, userName, readAtLocal);
      readDataList.add(readData);
    }
    return readDataList;
  }
}

