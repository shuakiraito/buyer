package com.study.r4a122.webportal.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 新規登録サービスクラスです。
 * 新規登録関連の操作を提供します。
 */
@Service
@Transactional
public class RegisterService {

  @Autowired
  private UserRepository userRepository;

  /**
   * 新規ユーザー登録を行います。
   *
   * @param userId   ユーザーID
   * @param password パスワード
   * @param userName ユーザー名
   * @param role     役割（ROLE_TEACHER または ROLE_STUDENT）
   * @return 登録成功時はnull、失敗時はエラーメッセージ
   */
  public String register(String userId, String password, String userName, String role) {
    // 入力値のバリデーション
    if (userId == null || userId.trim().isEmpty()) {
      return "ユーザーIDを入力してください。";
    }
    if (password == null || password.trim().isEmpty()) {
      return "パスワードを入力してください。";
    }
    if (userName == null || userName.trim().isEmpty()) {
      return "ユーザー名を入力してください。";
    }
    if (role == null || role.trim().isEmpty()) {
      return "役割を選択してください。";
    }
    if (password.length() < 4) {
      return "パスワードは4文字以上で入力してください。";
    }
    if (!role.equals("ROLE_TEACHER") && !role.equals("ROLE_STUDENT")) {
      return "無効な役割が選択されています。";
    }

    // ユーザーIDの重複チェック
    if (userRepository.existsByUserId(userId)) {
      return "このユーザーIDは既に使用されています。";
    }

    // ユーザー登録
    boolean success = userRepository.register(userId.trim(), password, userName.trim(), role);
    if (!success) {
      return "登録に失敗しました。再度お試しください。";
    }

    return null; // 成功時はnullを返す
  }
}

