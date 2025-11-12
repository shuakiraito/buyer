package com.study.r4a122.webportal.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
  @Autowired
  private NamedParameterJdbcTemplate jdbc;

  /**
   * 入力されたユーザーIDとパスワードにマッチするユーザーデータを取得します。
   *
   * @param userId   取得するユーザーデータのユーザーID
   * @param password 取得するユーザーデータのパスワード
   * @return 入力されたユーザーIDのユーザーデータ (存在しない場合はnull)
   */
  public UserData login(String userId, String password) {
    /** SQL ログインチェック */
    final String SQL_LOGIN = "SELECT user_name, role, enabled, locked, failed_attempts, locked_at, last_login_at FROM user_m WHERE user_id = :userId AND password = :password AND enabled = true";

    // パラメータを格納するためのマップを作成
    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);
    params.put("password", password);

    // データベースのクエリを実行し、結果を取得
    // queryForListは結果が0件の場合、空のListを返す
    List<Map<String, Object>> resultList = jdbc.queryForList(SQL_LOGIN, params);

    UserData userData = null;
    // ユーザーが1件だけ見つかった場合のみ処理を続行
    if (resultList.size() == 1) {
      Map<String, Object> item = resultList.get(0);
      String userName = (String) item.get("user_name");
      String role = (String) item.get("role");
      boolean enabled = (boolean) item.get("enabled");
      boolean locked = item.get("locked") != null && (Boolean) item.get("locked");
      Integer failedAttempts = item.get("failed_attempts") != null ? (Integer) item.get("failed_attempts") : 0;
      java.sql.Timestamp lockedAtTs = (java.sql.Timestamp) item.get("locked_at");
      java.sql.Timestamp lastLoginAtTs = (java.sql.Timestamp) item.get("last_login_at");

      userData = new UserData(
          userId,
          "****",
          userName,
          role,
          enabled,
          locked,
          failedAttempts != null ? failedAttempts : 0,
          lockedAtTs != null ? lockedAtTs.toLocalDateTime() : null,
          lastLoginAtTs != null ? lastLoginAtTs.toLocalDateTime() : null);
    }
    return userData;
  }

  /**
   * ユーザーIDが既に存在するかチェックします。
   *
   * @param userId チェックするユーザーID
   * @return 存在する場合はtrue、存在しない場合はfalse
   */
  public boolean existsByUserId(String userId) {
    final String SQL_CHECK = "SELECT COUNT(*) FROM user_m WHERE user_id = :userId";
    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);

    Integer count = jdbc.queryForObject(SQL_CHECK, params, Integer.class);
    return count != null && count > 0;
  }

  /**
   * 新規ユーザーを登録します。
   *
   * @param userId   ユーザーID
   * @param password パスワード
   * @param userName ユーザー名
   * @param role     役割（ROLE_TEACHER または ROLE_STUDENT）
   * @return 登録成功時はtrue、失敗時はfalse
   */
  public boolean register(String userId, String password, String userName, String role) {
    final String SQL_INSERT = "INSERT INTO user_m (user_id, PASSWORD, user_name, ROLE, enabled) VALUES (:userId, :password, :userName, :role, true)";
    
    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);
    params.put("password", password);
    params.put("userName", userName);
    params.put("role", role);

    try {
      int result = jdbc.update(SQL_INSERT, params);
      return result == 1;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * 全ユーザーを取得します。
   *
   * @return 全ユーザーのリスト
   */
  public List<UserData> findAll() {
    final String SQL_SELECT = "SELECT user_id, user_name, role, enabled, locked, failed_attempts, locked_at, last_login_at FROM user_m ORDER BY user_name";
    Map<String, Object> params = new HashMap<>();

    List<Map<String, Object>> resultList = jdbc.queryForList(SQL_SELECT, params);
    List<UserData> users = new java.util.ArrayList<>();

    for (Map<String, Object> map : resultList) {
      String userId = (String) map.get("user_id");
      String userName = (String) map.get("user_name");
      String role = (String) map.get("role");
      boolean enabled = map.get("enabled") != null && (Boolean) map.get("enabled");
      boolean locked = map.get("locked") != null && (Boolean) map.get("locked");
      Integer failedAttempts = map.get("failed_attempts") != null ? (Integer) map.get("failed_attempts") : 0;
      java.sql.Timestamp lockedAtTs = (java.sql.Timestamp) map.get("locked_at");
      java.sql.Timestamp lastLoginAtTs = (java.sql.Timestamp) map.get("last_login_at");
      users.add(new UserData(
          userId,
          "****",
          userName,
          role,
          enabled,
          locked,
          failedAttempts != null ? failedAttempts : 0,
          lockedAtTs != null ? lockedAtTs.toLocalDateTime() : null,
          lastLoginAtTs != null ? lastLoginAtTs.toLocalDateTime() : null));
    }
    return users;
  }

  /**
   * ユーザーIDでユーザー情報を取得します。
   *
   * @param userId ユーザーID
   * @return ユーザーデータ（存在しない場合はnull）
   */
  public UserData findByUserId(String userId) {
    final String SQL_SELECT = "SELECT user_id, user_name, role, enabled, locked, failed_attempts, locked_at, last_login_at FROM user_m WHERE user_id = :userId";
    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);

    List<Map<String, Object>> resultList = jdbc.queryForList(SQL_SELECT, params);
    if (resultList.isEmpty()) {
      return null;
    }

    Map<String, Object> map = resultList.get(0);
    String uid = (String) map.get("user_id");
    String userName = (String) map.get("user_name");
    String role = (String) map.get("role");
    boolean enabled = map.get("enabled") != null && (Boolean) map.get("enabled");
    boolean locked = map.get("locked") != null && (Boolean) map.get("locked");
    Integer failedAttempts = map.get("failed_attempts") != null ? (Integer) map.get("failed_attempts") : 0;
    java.sql.Timestamp lockedAtTs = (java.sql.Timestamp) map.get("locked_at");
    java.sql.Timestamp lastLoginAtTs = (java.sql.Timestamp) map.get("last_login_at");

    return new UserData(
        uid,
        "****",
        userName,
        role,
        enabled,
        locked,
        failedAttempts != null ? failedAttempts : 0,
        lockedAtTs != null ? lockedAtTs.toLocalDateTime() : null,
        lastLoginAtTs != null ? lastLoginAtTs.toLocalDateTime() : null);
  }

  public void resetFailedAttempts(String userId) {
    final String SQL_UPDATE = "UPDATE user_m SET failed_attempts = 0, locked = false, locked_at = NULL, last_login_at = CURRENT_TIMESTAMP WHERE user_id = :userId";
    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);
    jdbc.update(SQL_UPDATE, params);
  }

  public int incrementFailedAttempts(String userId) {
    final String SQL_UPDATE = "UPDATE user_m SET failed_attempts = failed_attempts + 1 WHERE user_id = :userId";
    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);
    return jdbc.update(SQL_UPDATE, params);
  }

  public void lockUser(String userId) {
    final String SQL_UPDATE = "UPDATE user_m SET locked = true, locked_at = CURRENT_TIMESTAMP WHERE user_id = :userId";
    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);
    jdbc.update(SQL_UPDATE, params);
  }

  public void unlockUser(String userId) {
    final String SQL_UPDATE = "UPDATE user_m SET locked = false, failed_attempts = 0, locked_at = NULL WHERE user_id = :userId";
    Map<String, Object> params = new HashMap<>();
    params.put("userId", userId);
    jdbc.update(SQL_UPDATE, params);
  }
}
