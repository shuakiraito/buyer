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
public class MessageFileRepository {

  @Autowired
  private NamedParameterJdbcTemplate jdbc;

  public List<MessageFileData> findByMessageId(int messageId) {
    final String SQL_SELECT = "SELECT id, message_id, file_name, file_path, file_size, file_type, uploaded_by, uploaded_at " +
        "FROM message_file_t WHERE message_id = :messageId";
    Map<String, Object> params = new HashMap<>();
    params.put("messageId", messageId);

    List<Map<String, Object>> resultList = jdbc.queryForList(SQL_SELECT, params);
    List<MessageFileData> files = new ArrayList<>();

    for (Map<String, Object> map : resultList) {
      int id = (Integer) map.get("id");
      int msgId = (Integer) map.get("message_id");
      String fileName = (String) map.get("file_name");
      String filePath = (String) map.get("file_path");
      Long fileSize = map.get("file_size") != null ? ((Number) map.get("file_size")).longValue() : null;
      String fileType = (String) map.get("file_type");
      String uploadedBy = (String) map.get("uploaded_by");
      Timestamp uploadedAt = (Timestamp) map.get("uploaded_at");

      LocalDateTime uploadedAtLocal = uploadedAt != null ? uploadedAt.toLocalDateTime() : LocalDateTime.now();
      MessageFileData file = new MessageFileData(id, msgId, fileName, filePath, fileSize, fileType, uploadedBy, uploadedAtLocal);
      files.add(file);
    }
    return files;
  }

  public int save(MessageFileData fileData) {
    final String SQL_INSERT = "INSERT INTO message_file_t(id, message_id, file_name, file_path, file_size, file_type, uploaded_by) " +
        "VALUES((SELECT COALESCE(MAX(id), 0) + 1 FROM message_file_t), :messageId, :fileName, :filePath, :fileSize, :fileType, :uploadedBy)";
    Map<String, Object> params = new HashMap<>();
    params.put("messageId", fileData.messageId());
    params.put("fileName", fileData.fileName());
    params.put("filePath", fileData.filePath());
    params.put("fileSize", fileData.fileSize());
    params.put("fileType", fileData.fileType());
    params.put("uploadedBy", fileData.uploadedBy());

    return jdbc.update(SQL_INSERT, params);
  }
}

