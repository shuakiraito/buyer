package com.study.r4a122.webportal.chat;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChannelInvitationRepository {

  @Autowired
  private NamedParameterJdbcTemplate jdbc;

  public ChannelInvitationData createInvitation(int channelId, String inviterId, String inviteeId) {
    final String SQL_INSERT = "INSERT INTO channel_invitation_t(id, channel_id, inviter_id, invitee_id) "
        + "VALUES((SELECT COALESCE(MAX(id), 0) + 1 FROM channel_invitation_t), :channelId, :inviterId, :inviteeId)";
    Map<String, Object> params = new HashMap<>();
    params.put("channelId", channelId);
    params.put("inviterId", inviterId);
    params.put("inviteeId", inviteeId);
    jdbc.update(SQL_INSERT, params);
    return findLatestInvitation(channelId, inviteeId);
  }

  public boolean updateStatus(int invitationId, String status) {
    final String SQL_UPDATE = "UPDATE channel_invitation_t SET status = :status, responded_at = CURRENT_TIMESTAMP WHERE id = :id";
    Map<String, Object> params = new HashMap<>();
    params.put("id", invitationId);
    params.put("status", status);
    return jdbc.update(SQL_UPDATE, params) == 1;
  }

  public boolean existsPending(int channelId, String inviteeId) {
    final String SQL = "SELECT COUNT(*) FROM channel_invitation_t "
        + "WHERE channel_id = :channelId AND invitee_id = :inviteeId AND status = 'PENDING'";
    Map<String, Object> params = new HashMap<>();
    params.put("channelId", channelId);
    params.put("inviteeId", inviteeId);
    Integer count = jdbc.queryForObject(SQL, params, Integer.class);
    return count != null && count > 0;
  }

  public List<ChannelInvitationData> findPendingByInvitee(String inviteeId) {
    final String SQL = baseSelectSql() + " WHERE ci.invitee_id = :inviteeId AND ci.status = 'PENDING' ORDER BY ci.created_at DESC";
    Map<String, Object> params = new HashMap<>();
    params.put("inviteeId", inviteeId);
    return query(sqlNamed(SQL), params);
  }

  public List<ChannelInvitationData> findByChannel(int channelId) {
    final String SQL = baseSelectSql() + " WHERE ci.channel_id = :channelId ORDER BY ci.created_at DESC";
    Map<String, Object> params = new HashMap<>();
    params.put("channelId", channelId);
    return query(sqlNamed(SQL), params);
  }

  public ChannelInvitationData findById(int invitationId) {
    final String SQL = baseSelectSql() + " WHERE ci.id = :id";
    Map<String, Object> params = new HashMap<>();
    params.put("id", invitationId);
    List<ChannelInvitationData> list = query(sqlNamed(SQL), params);
    return list.isEmpty() ? null : list.get(0);
  }

  private ChannelInvitationData findLatestInvitation(int channelId, String inviteeId) {
    final String SQL = baseSelectSql() + " WHERE ci.channel_id = :channelId AND ci.invitee_id = :inviteeId ORDER BY ci.created_at DESC";
    Map<String, Object> params = new HashMap<>();
    params.put("channelId", channelId);
    params.put("inviteeId", inviteeId);
    List<ChannelInvitationData> list = query(sqlNamed(SQL), params);
    return list.isEmpty() ? null : list.get(0);
  }

  private String baseSelectSql() {
    return "SELECT ci.id, ci.channel_id, ch.channel_name, ci.inviter_id, inviter.user_name AS inviter_name, "
        + "ci.invitee_id, invitee.user_name AS invitee_name, ci.status, ci.created_at, ci.responded_at "
        + "FROM channel_invitation_t ci "
        + "LEFT JOIN channel_t ch ON ci.channel_id = ch.id "
        + "LEFT JOIN user_m inviter ON ci.inviter_id = inviter.user_id "
        + "LEFT JOIN user_m invitee ON ci.invitee_id = invitee.user_id";
  }

  private List<ChannelInvitationData> query(String sql, Map<String, Object> params) {
    List<Map<String, Object>> result = jdbc.queryForList(sql, params);
    List<ChannelInvitationData> data = new ArrayList<>();
    for (Map<String, Object> row : result) {
      Integer id = (Integer) row.get("id");
      Integer channelId = (Integer) row.get("channel_id");
      String channelName = (String) row.get("channel_name");
      String inviterId = (String) row.get("inviter_id");
      String inviterName = (String) row.get("inviter_name");
      String inviteeId = (String) row.get("invitee_id");
      String inviteeName = (String) row.get("invitee_name");
      String status = (String) row.get("status");
      Timestamp createdAt = (Timestamp) row.get("created_at");
      Timestamp respondedAt = (Timestamp) row.get("responded_at");
      data.add(new ChannelInvitationData(
          id != null ? id : 0,
          channelId != null ? channelId : 0,
          channelName,
          inviterId,
          inviterName,
          inviteeId,
          inviteeName,
          status,
          createdAt != null ? createdAt.toLocalDateTime() : null,
          respondedAt != null ? respondedAt.toLocalDateTime() : null));
    }
    return data;
  }

  private String sqlNamed(String sql) {
    return sql;
  }
}

