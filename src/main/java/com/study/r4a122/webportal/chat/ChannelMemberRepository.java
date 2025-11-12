package com.study.r4a122.webportal.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChannelMemberRepository {

  @Autowired
  private NamedParameterJdbcTemplate jdbc;

  public boolean isMember(int channelId, String userId) {
    final String SQL_CHECK = "SELECT COUNT(*) FROM channel_member_t WHERE channel_id = :channelId AND user_id = :userId";
    Map<String, Object> params = new HashMap<>();
    params.put("channelId", channelId);
    params.put("userId", userId);

    Integer count = jdbc.queryForObject(SQL_CHECK, params, Integer.class);
    return count != null && count > 0;
  }

  public boolean joinChannel(int channelId, String userId) {
    if (isMember(channelId, userId)) {
      return false; // 既に参加している
    }

    final String SQL_INSERT = "INSERT INTO channel_member_t(id, channel_id, user_id) " +
        "VALUES((SELECT COALESCE(MAX(id), 0) + 1 FROM channel_member_t), :channelId, :userId)";
    Map<String, Object> params = new HashMap<>();
    params.put("channelId", channelId);
    params.put("userId", userId);

    return jdbc.update(SQL_INSERT, params) == 1;
  }

  public boolean leaveChannel(int channelId, String userId) {
    final String SQL_DELETE = "DELETE FROM channel_member_t WHERE channel_id = :channelId AND user_id = :userId";
    Map<String, Object> params = new HashMap<>();
    params.put("channelId", channelId);
    params.put("userId", userId);

    return jdbc.update(SQL_DELETE, params) == 1;
  }

  public List<String> getChannelMembers(int channelId) {
    final String SQL_SELECT = "SELECT user_id FROM channel_member_t WHERE channel_id = :channelId";
    Map<String, Object> params = new HashMap<>();
    params.put("channelId", channelId);

    List<Map<String, Object>> resultList = jdbc.queryForList(SQL_SELECT, params);
    List<String> members = new ArrayList<>();

    for (Map<String, Object> map : resultList) {
      String userId = (String) map.get("user_id");
      members.add(userId);
    }
    return members;
  }

  public void addCreatorAsMember(int channelId, String userId) {
    joinChannel(channelId, userId);
  }
}

