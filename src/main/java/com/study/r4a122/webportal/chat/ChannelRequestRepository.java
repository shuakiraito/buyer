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
public class ChannelRequestRepository {

  @Autowired
  private NamedParameterJdbcTemplate jdbc;

  public List<ChannelRequestData> findAllPending() {
    final String SQL_SELECT = "SELECT cr.id, cr.channel_name, cr.description, cr.is_public, cr.requested_by, u.user_name, " +
        "cr.status, cr.created_at, cr.reviewed_by, cr.reviewed_at " +
        "FROM channel_request_t cr " +
        "LEFT JOIN user_m u ON cr.requested_by = u.user_id " +
        "WHERE cr.status = 'PENDING' " +
        "ORDER BY cr.created_at DESC";
    Map<String, Object> params = new HashMap<>();

    return executeRequestQuery(SQL_SELECT, params);
  }

  public ChannelRequestData findById(int requestId) {
    final String SQL_SELECT = "SELECT cr.id, cr.channel_name, cr.description, cr.is_public, cr.requested_by, u.user_name, " +
        "cr.status, cr.created_at, cr.reviewed_by, cr.reviewed_at " +
        "FROM channel_request_t cr " +
        "LEFT JOIN user_m u ON cr.requested_by = u.user_id " +
        "WHERE cr.id = :id";
    Map<String, Object> params = new HashMap<>();
    params.put("id", requestId);

    List<ChannelRequestData> results = executeRequestQuery(SQL_SELECT, params);
    return results.isEmpty() ? null : results.get(0);
  }

  private List<ChannelRequestData> executeRequestQuery(String sql, Map<String, Object> params) {
    List<Map<String, Object>> resultList = jdbc.queryForList(sql, params);
    List<ChannelRequestData> requests = new ArrayList<>();

    for (Map<String, Object> map : resultList) {
      int id = (Integer) map.get("id");
      String channelName = (String) map.get("channel_name");
      String description = (String) map.get("description");
      boolean isPublic = map.get("is_public") != null && (Boolean) map.get("is_public");
      String requestedBy = (String) map.get("requested_by");
      String requestedByName = (String) map.get("user_name");
      String status = (String) map.get("status");
      Timestamp createdAt = (Timestamp) map.get("created_at");
      String reviewedBy = (String) map.get("reviewed_by");
      Timestamp reviewedAt = (Timestamp) map.get("reviewed_at");

      LocalDateTime createdAtLocal = createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now();
      LocalDateTime reviewedAtLocal = reviewedAt != null ? reviewedAt.toLocalDateTime() : null;
      ChannelRequestData request = new ChannelRequestData(id, channelName, description, isPublic, requestedBy, 
          requestedByName, status, createdAtLocal, reviewedBy, reviewedAtLocal);
      requests.add(request);
    }
    return requests;
  }

  public int save(ChannelRequestData requestData) {
    final String SQL_INSERT = "INSERT INTO channel_request_t(id, channel_name, description, is_public, requested_by, status) " +
        "VALUES((SELECT COALESCE(MAX(id), 0) + 1 FROM channel_request_t), :channelName, :description, :isPublic, :requestedBy, 'PENDING')";
    Map<String, Object> params = new HashMap<>();
    params.put("channelName", requestData.channelName());
    params.put("description", requestData.description());
    params.put("isPublic", requestData.isPublic());
    params.put("requestedBy", requestData.requestedBy());

    return jdbc.update(SQL_INSERT, params);
  }

  public boolean updateStatus(int requestId, String status, String reviewedBy) {
    final String SQL_UPDATE = "UPDATE channel_request_t SET status = :status, reviewed_by = :reviewedBy, reviewed_at = CURRENT_TIMESTAMP " +
        "WHERE id = :requestId";
    Map<String, Object> params = new HashMap<>();
    params.put("requestId", requestId);
    params.put("status", status);
    params.put("reviewedBy", reviewedBy);

    return jdbc.update(SQL_UPDATE, params) == 1;
  }
}

