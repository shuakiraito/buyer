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
public class ChannelRepository {

  @Autowired
  private NamedParameterJdbcTemplate jdbc;

  public int nextId() {
    final String SQL_NEXT = "SELECT COALESCE(MAX(id), 0) + 1 FROM channel_t";
    Integer next = jdbc.queryForObject(SQL_NEXT, Map.of(), Integer.class);
    return next != null ? next : 1;
  }

  public List<ChannelData> findAll() {
    final String SQL_SELECT_ALL = "SELECT id, channel_name, description, is_public, status, created_at, created_by FROM channel_t WHERE status = 'ACTIVE' ORDER BY created_at DESC";
    Map<String, Object> params = new HashMap<>();

    List<Map<String, Object>> resultList = jdbc.queryForList(SQL_SELECT_ALL, params);
    List<ChannelData> channels = new ArrayList<>();

    for (Map<String, Object> map : resultList) {
      int id = (Integer) map.get("id");
      String channelName = (String) map.get("channel_name");
      String description = (String) map.get("description");
      boolean isPublic = map.get("is_public") != null && (Boolean) map.get("is_public");
      String status = (String) map.get("status");
      Timestamp createdAt = (Timestamp) map.get("created_at");
      String createdBy = (String) map.get("created_by");

      LocalDateTime createdAtLocal = createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now();
      ChannelData channel = new ChannelData(id, channelName, description, isPublic, status, createdAtLocal, createdBy);
      channels.add(channel);
    }
    return channels;
  }

  public ChannelData findById(int channelId) {
    final String SQL_SELECT_ONE = "SELECT id, channel_name, description, is_public, status, created_at, created_by FROM channel_t WHERE id = :id";
    Map<String, Object> params = new HashMap<>();
    params.put("id", channelId);

    List<Map<String, Object>> resultList = jdbc.queryForList(SQL_SELECT_ONE, params);
    if (resultList.isEmpty()) {
      return null;
    }

    Map<String, Object> map = resultList.get(0);
    int id = (Integer) map.get("id");
    String channelName = (String) map.get("channel_name");
    String description = (String) map.get("description");
    boolean isPublic = map.get("is_public") != null && (Boolean) map.get("is_public");
    String status = (String) map.get("status");
    Timestamp createdAt = (Timestamp) map.get("created_at");
    String createdBy = (String) map.get("created_by");

    LocalDateTime createdAtLocal = createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now();
    return new ChannelData(id, channelName, description, isPublic, status, createdAtLocal, createdBy);
  }

  public int save(ChannelData channelData) {
    final String SQL_INSERT = "INSERT INTO channel_t(id, channel_name, description, is_public, status, created_by) " +
        "VALUES(:id, :channelName, :description, :isPublic, :status, :createdBy)";
    Map<String, Object> params = new HashMap<>();
    params.put("id", channelData.id());
    params.put("channelName", channelData.channelName());
    params.put("description", channelData.description());
    params.put("isPublic", channelData.isPublic());
    params.put("status", channelData.status());
    params.put("createdBy", channelData.createdBy());

    return jdbc.update(SQL_INSERT, params);
  }

  public ChannelData findByNameAndCreator(String channelName, String createdBy) {
    final String SQL_SELECT = "SELECT id, channel_name, description, is_public, status, created_at, created_by "
        + "FROM channel_t WHERE channel_name = :channelName AND created_by = :createdBy ORDER BY created_at DESC";
    Map<String, Object> params = new HashMap<>();
    params.put("channelName", channelName);
    params.put("createdBy", createdBy);

    List<Map<String, Object>> resultList = jdbc.queryForList(SQL_SELECT, params);
    if (resultList.isEmpty()) {
      return null;
    }
    Map<String, Object> map = resultList.get(0);
    int id = (Integer) map.get("id");
    String description = (String) map.get("description");
    boolean isPublic = map.get("is_public") != null && (Boolean) map.get("is_public");
    String status = (String) map.get("status");
    Timestamp createdAt = (Timestamp) map.get("created_at");
    String created = (String) map.get("created_by");
    LocalDateTime createdAtLocal = createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now();
    return new ChannelData(id, channelName, description, isPublic, status, createdAtLocal, created);
  }

  public boolean update(int channelId, String name, String description, boolean isPublic) {
    final String SQL_UPDATE = "UPDATE channel_t SET channel_name = :channelName, description = :description, is_public = :isPublic WHERE id = :channelId";
    Map<String, Object> params = new HashMap<>();
    params.put("channelId", channelId);
    params.put("channelName", name);
    params.put("description", description);
    params.put("isPublic", isPublic);
    return jdbc.update(SQL_UPDATE, params) == 1;
  }

  public boolean softDelete(int channelId) {
    final String SQL_UPDATE = "UPDATE channel_t SET status = 'DELETED' WHERE id = :channelId";
    Map<String, Object> params = new HashMap<>();
    params.put("channelId", channelId);
    return jdbc.update(SQL_UPDATE, params) == 1;
  }
}

