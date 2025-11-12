package com.study.r4a122.webportal.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ログインサービスクラスです。
 * ログイン関連の操作を提供します。
 *
 * @author 情報太郎
 */
@Service
@Transactional
public class LoginService {

  // セッションにユーザー情報を格納する際のキーとなる定数
  private final String SESSION_USER_DATA_KEY = "userData";

  /** セッションオブジェクト */
  @Autowired
  private HttpSession session;

  /** ユーザーリポジトリ */
  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RememberMeService rememberMeService;

  /**
   * ログイン処理を行います。
   *
   * @param userId   ユーザーID
   * @param password パスワード
   * @return ログイン成功時はtrue、失敗時はfalse
   */
  public LoginResult login(String userId, String password, boolean rememberMe, HttpServletResponse response) {
    UserData existing = userRepository.findByUserId(userId);
    if (existing == null) {
      return LoginResult.error("ユーザIDまたはパスワードが違います。");
    }
    if (!existing.enabled()) {
      return LoginResult.error("このアカウントは無効です。管理者にお問い合わせください。");
    }
    if (existing.locked()) {
      return LoginResult.lockedResult("アカウントがロックされています。管理者に連絡してください。");
    }

    UserData userData = userRepository.login(userId, password);

    if (userData == null) {
      userRepository.incrementFailedAttempts(userId);
      UserData after = userRepository.findByUserId(userId);
      if (after != null && after.failedAttempts() >= 5) {
        userRepository.lockUser(userId);
        return LoginResult.lockedResult("ログインに5回連続で失敗したため、アカウントをロックしました。管理者に連絡してください。");
      }
      return LoginResult.error("ユーザIDまたはパスワードが違います。");
    }

    userRepository.resetFailedAttempts(userId);
    session.setAttribute(SESSION_USER_DATA_KEY, userData);

    if (rememberMe) {
      rememberMeService.issueToken(userId, response);
    } else {
      rememberMeService.clearToken(userId, response);
    }

    return LoginResult.ok();
  }

  /**
   * ログアウト処理を行います。
   */
  public void logout() {
    // ログイン情報を破棄
    session.invalidate();
  }

  public void logout(HttpServletResponse response) {
    UserData current = getUserData();
    if (current != null) {
      rememberMeService.clearToken(current.userId(), response);
    }
    logout();
  }

  /**
   * ログインユーザーIDを取得します。
   *
   * @return ユーザーID
   */
  public String getLoginUserId() {
    UserData userData = (UserData) session.getAttribute(SESSION_USER_DATA_KEY);

    if (userData == null) {
      return null; // セッションにユーザー情報がない場合
    }

    return userData.userId(); // ユーザーIDを取得
  }

  /**
   * ログインユーザーデータを取得します。
   *
   * @return ユーザーデータ
   */
  public UserData getUserData() {
    return (UserData) session.getAttribute(SESSION_USER_DATA_KEY);
  }

  /**
   * Remember-Me Cookie からの自動ログインを試みます。
   *
   * @param request  HTTP リクエスト
   * @param response HTTP レスポンス
   * @return ログイン済みのユーザーデータ（成功時）
   */
  public UserData autoLogin(HttpServletRequest request, HttpServletResponse response) {
    UserData sessionUser = getUserData();
    if (sessionUser != null) {
      return sessionUser;
    }
    UserData rememberMeUser = rememberMeService.autoLogin(request, response);
    if (rememberMeUser != null) {
      session.setAttribute(SESSION_USER_DATA_KEY, rememberMeUser);
    }
    return rememberMeUser;
  }
}
