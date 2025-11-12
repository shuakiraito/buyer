package com.study.r4a122.webportal.task;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.study.r4a122.webportal.user.LoginService;

@Controller
public class TaskController {

  @Autowired
  private LoginService loginService;

  @Autowired
  private TaskService taskService;

  /**
   * タスク一覧を表示します。
   */
  @GetMapping("/task")
  public String getTaskList(Model model) {
    String userId = loginService.getLoginUserId();
    List<TaskData> tasks = taskService.selectAll(userId);
    model.addAttribute("tasks", tasks);
    return "task/list";
  }

  /**
   * タスクを登録します。
   */
  @PostMapping("/task/insert")
  public String insertTask(
      @RequestParam(name = "title") String title,
      @RequestParam(name = "limit") String limit,
      Model model,
      RedirectAttributes redirectAttributes) {

    boolean isValid = taskService.validate(title, limit);
    if (!isValid) {
      model.addAttribute("errorMessage", "入力項目に不備があります");
      return getTaskList(model);
    }

    boolean isSuccess = taskService.insert(loginService.getLoginUserId(), title, limit);

    if (isSuccess) {
      redirectAttributes.addFlashAttribute("message", "正常に登録されました");
    } else {
      redirectAttributes.addFlashAttribute("errorMessage", "登録できませんでした。再度登録し直してください");
    }

    return "redirect:/task";
  }

  /**
   * タスクを完了状態に変更します（ユーザー所有チェックあり）。
   */
  @PostMapping("/task/complete")
  public String completeTask(
      @RequestParam(name = "id") String id,
      RedirectAttributes redirectAttributes) {

    Long taskId;
    try {
      taskId = Long.parseLong(id);
    } catch (NumberFormatException e) {
      redirectAttributes.addFlashAttribute("errorMessage", "不正なタスクIDです。");
      return "redirect:/task";
    }

    boolean isSuccess = taskService.completeTask(loginService.getLoginUserId(), taskId);

    if (isSuccess) {
      redirectAttributes.addFlashAttribute("message", "タスクが完了しました。");
    } else {
      redirectAttributes.addFlashAttribute("errorMessage", "タスクを完了できませんでした。");
    }

    return "redirect:/task";
  }

  /**
   * タスクを削除します（ユーザー所有チェックあり）。
   */
  @PostMapping("/task/delete")
  public String deleteTask(
      @RequestParam(name = "id") String id,
      RedirectAttributes redirectAttributes) {

    if (id == null || id.isBlank()) {
      redirectAttributes.addFlashAttribute("errorMessage", "タスクIDが指定されていません。");
      return "redirect:/task";
    }

    boolean isSuccess = taskService.delete(loginService.getLoginUserId(), id);

    if (isSuccess) {
      redirectAttributes.addFlashAttribute("message", "タスクを削除しました。");
    } else {
      redirectAttributes.addFlashAttribute("errorMessage", "タスクを削除できませんでした。");
    }

    return "redirect:/task";
  }

  /**
   * タスク一覧をCSV形式でダウンロードします。
   */
  @GetMapping("/task/download")
  public void downloadCsv(HttpServletResponse response) {
    String userId = loginService.getLoginUserId();
    List<TaskData> tasks = taskService.selectAll(userId);

    response.setContentType("text/csv; charset=UTF-8");

    String filename = "tasks.csv";
    String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
    response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);

    try (var writer = response.getWriter()) {
      // ヘッダー行
      writer.println("ID,タイトル,期限,完了");

      for (TaskData task : tasks) {
        writer.printf(
            "%d,%s,%s,%s%n",
            task.id(), // getId() → id()
            escapeCsv(task.title()), // getTitle() → title()
            task.limitday(), // getLimitday() → limitday()
            task.completed() ? "完了" : "未完了" // isDone() → completed()
        );
      }

    } catch (Exception e) {
      throw new RuntimeException("CSV出力中にエラーが発生しました", e);
    }
  }

  /**
   * CSVのエスケープ処理（カンマ、ダブルクオート、改行を含む場合に対応）
   */
  private String escapeCsv(String field) {
    if (field == null)
      return "";
    if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
      return "\"" + field.replace("\"", "\"\"") + "\"";
    }
    return field;
  }
}
