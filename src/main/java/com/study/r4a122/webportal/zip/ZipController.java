package com.study.r4a122.webportal.zip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.client.RestClientException;

/**
 * 住所検索に関する機能を管理するコントローラークラスです。
 * 主に住所検索画面（入力画面）の表示を担当します。
 */
@Controller
public class ZipController {

  @Autowired
  private ZipService zipService;

  /**
   * 住所検索画面の表示
   */
  @GetMapping("/zip")
  public String getZip() {
    return "zip/input"; // zip/input.html が表示される
  }

  /**
   * 郵便番号で住所検索し、結果を表示
   */
  @PostMapping("/zip")
  public String postZip(@RequestParam(name = "zipcode") String zipcode, Model model) {
    try {
      ZipData zipData = zipService.execute(zipcode);
      model.addAttribute("zipData", zipData);
    } catch (RestClientException | JsonProcessingException e) {
      model.addAttribute("errorMessage", "住所情報の取得に失敗しました。");
    }
    return "zip/result";
  }
}
