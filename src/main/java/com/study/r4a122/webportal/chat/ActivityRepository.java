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
public class ActivityRepository {

  @Autowired
  private NamedParameterJdbcTemplate jdbc;

  public List<ActivityData> findByUserId(String userId) {
    final String SQL_SELECT = "SELECT a.id, a.user_id, u.user_name, a.activity_type, " +
        "a.activity_message, a.related_id, a.related_type, a.created_at " +
        "FROM activity_t a " +
        "LEFT JOIN user_m u ON a.user_id = u.user_id " +
        "WHERE a.user_id = :userId " +
        "ORDER BY a.created_at DESC " +
        "LIMIT 50";
    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);

    List<Map<String, Object>> resultList = jdbc.queryForList(SQL_SELECT, params);
    List<ActivityData> activities = new ArrayList<>();

    for (Map<String, Object> map : resultList) {
      int id = (Integer) map.get("id");
      String uid = (String) map.get("user_id");
      String userName = (String) map.get("user_name");
      String activityType = (String) map.get("activity_type");
      String activityMessage = (String) map.get("activity_message");
      Integer relatedId = (Integer) map.get("related_id");
      String relatedType = (String) map.get("related_type");
      Timestamp createdAt = (Timestamp) map.get("created_at");

      LocalDateTime createdAtLocal = createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now();
      ActivityData activity = new ActivityData(id, uid, userName, activityType, activityMessage,
          relatedId, relatedType, createdAtLocal);
      activities.add(activity);
    }
    return activities;
  }

  public int save(ActivityData activityData) {
    final String SQL_INSERT = "INSERT INTO activity_t(id, user_id, activity_type, activity_message, related_id, related_type) " +
        "VALUES((SELECT COALESCE(MAX(id), 0) + 1 FROM activity_t), :userId, :activityType, :activityMessage, :relatedId, :relatedType)";
    Map<String, Object> params = new HashMap<>();
    params.put("userId", activityData.userId());
    params.put("activityType", activityData.activityType());
    params.put("activityMessage", activityData.activityMessage());
    params.put("relatedId", activityData.relatedId());
    params.put("relatedType", activityData.relatedType());

    return jdbc.update(SQL_INSERT, params);
  }
}

