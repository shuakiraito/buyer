package com.study.r4a122.webportal.practice;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

@Controller
public class PracticeController {

  @GetMapping("/dojo")
  public String goDojo() {
    return "practice/dojo";
  }

  /**
   * Level0の処理を行います。
   *
   * @return アルゴリズム道場画面へのパス
   */
  @PostMapping("/level0")
  public String level0() {
    String local = "jouhou";
    String domain = "hcs.ac.jp";

    System.out.println(local + "@" + domain);
    return "practice/dojo";
  }

  /**
   * Level1の処理を行います。
   *
   * @param model HTMLに値を渡すオブジェクト
   * @return アルゴリズム道場結果画面へのパス
   */
  @PostMapping("/level1")
  public String level1(Model model) {
    String input = "180 2";

    String[] xy = input.split(" ");
    int x = Integer.parseInt(xy[0]);
    int y = Integer.parseInt(xy[1]);

    model.addAttribute("ans", 100 + (x * y));

    return "practice/result";
  }

  /**
   * Level2の処理を行います。
   *
   * @param model HTMLに値を渡すオブジェクト
   * @return アルゴリズム道場結果画面へのパス
   */
  @PostMapping("/level2")
  public String level2(Model model) {
    String str = "namae";
    model.addAttribute("ans", str.substring(str.length() - 1));
    return "practice/result";
  }

  /**
   * Level3の処理を行います。
   *
   * @param input 入力文字列
   * @param model HTMLに値を渡すオブジェクト
   * @return アルゴリズム道場結果画面へのパス
   */
  @PostMapping("/level3")
  public String level3(@RequestParam(name = "level3") String input, Model model) {

    String ans = input.replaceAll("[aeiouAEIOU]", "");

    // String input = ;
    // replaceAll("['a','i','u'','e','o','A','I','U','E','O']","");

    model.addAttribute("ans", ans);

    return "practice/result";
  }

  /**
   * Level4の処理を行います。
   *
   * @param input 入力文字列
   * @param model HTMLに値を渡すオブジェクト
   * @return アルゴリズム道場結果画面へのパス
   */
  @PostMapping("/level4")
  public String level4(@RequestParam(name = "level4") String input, Model model) {
    String result;

    if (input.contains("ooo")) {
      result = "o";
    } else if (input.contains("xxx")) {
      result = "x";
    } else {
      result = "draw";
    }

    model.addAttribute("ans", result);

    return "practice/result";
  }
}
