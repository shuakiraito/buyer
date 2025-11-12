package com.study.r4a122.webportal.chat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChannelStarRepository {

  @Autowired
  private NamedParameterJdbcTemplate jdbc;

  public boolean isStarred(int channelId, String userId) {
    final String SQL_CHECK = "SELECT COUNT(*) FROM channel_star_t WHERE channel_id = :channelId AND user_id = :userId";
    Map<String, Object> params = new HashMap<>();
    params.put("channelId", channelId);
    params.put("userId", userId);

    Integer count = jdbc.queryForObject(SQL_CHECK, params, Integer.class);
    return count != null && count > 0;
  }

  public boolean toggleStar(int channelId, String userId) {
    if (isStarred(channelId, userId)) {
      // スターを削除
      final String SQL_DELETE = "DELETE FROM channel_star_t WHERE channel_id = :channelId AND user_id = :userId";
      Map<String, Object> params = new HashMap<>();
      params.put("channelId", channelId);
      params.put("userId", userId);
      return jdbc.update(SQL_DELETE, params) == 1;
    } else {
      // スターを追加
      final String SQL_INSERT = "INSERT INTO channel_star_t(id, channel_id, user_id) " +
          "VALUES((SELECT COALESCE(MAX(id), 0) + 1 FROM channel_star_t), :channelId, :userId)";
      Map<String, Object> params = new HashMap<>();
      params.put("channelId", channelId);
      params.put("userId", userId);
      return jdbc.update(SQL_INSERT, params) == 1;
    }
  }

  public List<Integer> findStarredChannels(String userId) {
    final String SQL_SELECT = "SELECT channel_id FROM channel_star_t WHERE user_id = :userId";
    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);

    return jdbc.queryForList(SQL_SELECT, params, Integer.class);
  }
}

