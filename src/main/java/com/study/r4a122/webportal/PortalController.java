package com.study.r4a122.webportal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PortalController {

  @GetMapping("/")
  public String index() {
    // ダッシュボードをスキップしてチャット画面へリダイレクト
    return "redirect:/chat";
  }
}
