package com.study.r4a122.webportal.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RegisterController {

  @Autowired
  private RegisterService registerService;
  
  @Autowired
  private SimpMessagingTemplate messagingTemplate;
  
  @Autowired
  private UserRepository userRepository;

  /**
   * 新規登録画面を表示します。
   *
   * @return 新規登録画面のテンプレート名
   */
  @GetMapping("/register")
  public String getRegister() {
    return "register";
  }

  /**
   * 新規登録処理を行います。
   *
   * @param userId   ユーザーID
   * @param password パスワード
   * @param userName ユーザー名
   * @param model    Viewに渡すデータを格納するオブジェクト
   * @return 登録成功時はログイン画面へリダイレクト、失敗時は新規登録画面を再表示
   */
  @PostMapping("/register")
  public String register(
      @RequestParam(name = "user_id") String userId,
      @RequestParam(name = "password") String password,
      @RequestParam(name = "user_name") String userName,
      @RequestParam(name = "role") String role,
      Model model) {
    
    // 新規登録処理の実行
    String errorMessage = registerService.register(userId, password, userName, role);
    
    if (errorMessage != null) {
      // エラーがある場合は新規登録画面を再表示
      model.addAttribute("errorMessage", errorMessage);
      return "register";
    }
    
    // 登録成功時、新規ユーザー情報を取得してWebSocketでブロードキャスト
    UserData newUser = userRepository.findByUserId(userId);
    if (newUser != null) {
      // パスワード情報を除外した安全なユーザー情報を作成
      // UserDataはrecordなので、全フィールドが必要だが、パスワードは"****"でマスク
      UserData safeUserData = new UserData(
          newUser.userId(),
          "****", // パスワードは送信しない（マスク）
          newUser.userName(),
          newUser.role(),
          newUser.enabled(),
          newUser.locked(),
          newUser.failedAttempts(),
          newUser.lockedAt(),
          newUser.lastLoginAt()
      );
      // 全クライアントに新規ユーザー登録を通知
      messagingTemplate.convertAndSend("/topic/users.activity", safeUserData);
    }
    
    // 成功時はログイン画面へリダイレクト（成功メッセージ付き）
    model.addAttribute("successMessage", "登録が完了しました。ログインしてください。");
    return "redirect:/login?success=true";
  }
}

