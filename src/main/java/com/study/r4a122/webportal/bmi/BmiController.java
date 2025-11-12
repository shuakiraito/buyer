package com.study.r4a122.webportal.bmi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class BmiController {

  @Autowired
  private BmiService bmiService;

  @GetMapping("/bmi")
  public String getBmi() {

    return "bmi/input";
  }

  @PostMapping("/bmi")
  public String postBmi(
      Model model,
      @RequestParam(name = "cm") String height,
      @RequestParam(name = "kg") String weight) {
    // 入力チェック
    boolean isVaild = bmiService.validate(height, weight);
    if (!isVaild) {
      // 入力チェックエラーの場合、前画面へ
      return "bmi/input";
    }
    // データ取得
    BmiData data = bmiService.execute(height, weight);
    // データをモデルオブジェクトに固定
    model.addAttribute("bmi", data);
    // 画面を返却
    return "bmi/result";
  }

}
