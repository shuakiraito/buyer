package com.study.r4a122.webportal.task;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

@Repository
public class TaskRepository {

  @Autowired
  private NamedParameterJdbcTemplate jdbc;

  public TaskEntity findTasksByUserId(String userId) {
    final String SQL_SELECT_ALL = "SELECT id, title, limitday, complete FROM task_t WHERE user_id = :userId ORDER BY limitday";
    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);

    List<Map<String, Object>> resultList = jdbc.queryForList(SQL_SELECT_ALL, params);
    TaskEntity entity = new TaskEntity();

    for (Map<String, Object> map : resultList) {
      int id = (Integer) map.get("id");
      String title = (String) map.get("title");
      Date limitday = (Date) map.get("limitday");
      boolean complete = (Boolean) map.get("complete");

      TaskData data = new TaskData(id, userId, title, limitday, complete);
      entity.taskList().add(data);
    }
    return entity;
  }

  public int save(TaskData taskData) throws SQLException {
    final String SQL_INSERT_ONE = "INSERT INTO task_t(id, user_id, title, limitday, complete) " +
        "VALUES((SELECT COALESCE(MAX(id), 0) + 1 FROM task_t), :userId, :title, :limitday, false)";
    Map<String, Object> params = new HashMap<>();
    params.put("userId", taskData.userId());
    params.put("title", taskData.title());
    params.put("limitday", taskData.limitday());

    int updateRow = jdbc.update(SQL_INSERT_ONE, params);
    if (updateRow != 1) {
      throw new SQLException("更新に失敗しました 件数:" + updateRow);
    }
    return updateRow;
  }

  public int delete(int id) throws SQLException {
    final String SQL_DELETE_ONE = "DELETE FROM task_t WHERE id = :id";
    Map<String, Object> params = new HashMap<>();
    params.put("id", id);

    int updateRow = jdbc.update(SQL_DELETE_ONE, params);
    if (updateRow != 1) {
      throw new SQLException("削除に失敗しました 件数:" + updateRow);
    }
    return updateRow;
  }

  public int update(int id) throws SQLException {
    final String SQL_UPDATE_ONE = "UPDATE task_t SET complete = true WHERE id = :id";
    Map<String, Object> params = new HashMap<>();
    params.put("id", id);

    int updateRow = jdbc.update(SQL_UPDATE_ONE, params);
    if (updateRow != 1) {
      throw new SQLException("更新に失敗しました 件数:" + updateRow);
    }
    return updateRow;
  }

  /**
   * CSV出力用のデータを処理するためのRowCallbackHandler実行。
   *
   * @param userId  対象ユーザーID
   * @param handler 処理対象のRowCallbackHandler
   */
  public void exportTasksToCsvByUserId(String userId, RowCallbackHandler handler) {
    final String SQL_EXPORT = "SELECT id, user_id, title, limitday " +
        "FROM task_t WHERE user_id = :userId ORDER BY limitday";
    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);

    jdbc.query(SQL_EXPORT, params, handler);
  }
}
