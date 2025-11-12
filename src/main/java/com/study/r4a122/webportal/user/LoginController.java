package com.study.r4a122.webportal.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // ← 修正点: Modelクラスをimport
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class LoginController {

  // ↓ 修正点: 型名と変数名のタイポを修正 (LoginServive -> LoginService)
  @Autowired
  private LoginService loginService;

  /**
   * ログイン画面を表示します。
   *
   * @return ログイン画面のテンプレート名
   */
  @GetMapping("/login")
  public String getLogin(
      @RequestParam(name = "success", required = false) String success,
      Model model,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (success != null && success.equals("true")) {
      model.addAttribute("successMessage", "登録が完了しました。ログインしてください。");
    }
    UserData autoLoginUser = loginService.autoLogin(request, response);
    if (autoLoginUser != null) {
      return "redirect:/chat";
    }
    return "login";
  }

  /**
   * ログイン処理を行います。
   *
   * @param userId   ユーザID (フォームの'user_id'から受け取る)
   * @param password パスワード (フォームの'password'から受け取る)
   * @param model    Viewに渡すデータを格納するオブジェクト
   * @return ログイン成功時はトップページへリダイレクト、失敗時はログイン画面を再表示
   */
  @PostMapping("/login")
  public String login(
      @RequestParam(name = "user_id") String userId,
      @RequestParam(name = "password") String password,
      @RequestParam(name = "remember_me", required = false, defaultValue = "false") boolean rememberMe,
      Model model,
      HttpServletResponse response) {
    // ログイン処理の実行
    LoginResult result = loginService.login(userId, password, rememberMe, response);
    if (!result.success()) {
      model.addAttribute("errorMessage", result.message() != null ? result.message() : "ユーザIDまたはパスワードが違います。");
      return "login"; // 失敗時は再度ログイン画面を表示
    }
    // 成功時はチャット画面へリダイレクト
    return "redirect:/chat";
  }

  /**
   * ログアウト処理を行います。
   *
   * @return ログイン画面
   */
  @GetMapping("/logout") // ← 修正点: "/loguot" を "/logout" に修正
  public String logout(HttpServletResponse response) { // ← 修正点: メソッド名を "loguot" から "logout" に修正
    // ログアウト処理
    loginService.logout(response);
    return "login"; // ログアウト後はログイン画面へ遷移
  }
}
