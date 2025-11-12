package com.study.r4a122.webportal.bmi;

import org.springframework.stereotype.Service;

@Service
public class BmiService {

  public BmiData execute(String height, String weight) {
    BmiData bmiData = new BmiData();

    // Validate input data before processing
    if (!validate(height, weight)) {
      bmiData.setAns("Invalid input");
      bmiData.setComment("Please provide valid height and weight.");
      return bmiData;
    }

    // BMI計算
    String ans = calc(height, weight);
    bmiData.setAns(ans);

    // 評価コメント
    String comment = judge(ans);
    bmiData.setComment(comment);

    // 画像パスを格納
    String img = img(ans);
    bmiData.setPath(img);

    return bmiData;
  }

  /**
   * Returns an image path based on the BMI result.
   *
   * @param ans The calculated BMI
   * @return Image path
   */
  private String img(String ans) {
    double bmi = Double.parseDouble(ans);
    String img;
    if (bmi < 18.50) {
      img = "/img/bmi/gari.png"; // Underweight
    } else if (bmi < 25.00) {
      img = "/img/bmi/normal.png"; // Normal weight
    } else {
      img = "/img/bmi/puni.png"; // Overweight
    }
    return img;
  }

  /**
   * Provides a BMI classification comment based on the result.
   *
   * @param ans The calculated BMI
   * @return A comment about the BMI
   */
  private String judge(String ans) {
    double bmi = Double.parseDouble(ans);
    String comment;
    if (bmi < 16.00) {
      comment = "痩せすぎ"; // Too thin
    } else if (bmi < 17.00) {
      comment = "痩せ"; // Thin
    } else if (bmi < 18.50) {
      comment = "痩せぎみ"; // Slightly underweight
    } else if (bmi < 25.00) {
      comment = "普通体重"; // Normal weight
    } else if (bmi < 30.00) {
      comment = "前肥満"; // Pre-obesity
    } else if (bmi < 35.00) {
      comment = "肥満(1度)"; // Obesity (Class 1)
    } else if (bmi < 40.00) {
      comment = "肥満(2度)"; // Obesity (Class 2)
    } else {
      comment = "肥満(3度)"; // Obesity (Class 3)
    }
    return comment;
  }

  /**
   * BMI計算を行います。
   *
   * @param height 身長
   * @param weight 体重
   * @return 計算したBMI
   */
  private String calc(String height, String weight) {
    try {
      // 身長をセンチメートルからメートルへ返還
      double m = Double.parseDouble(height) / 100;
      // BMIを計算
      double bmi = Double.parseDouble(weight) / (m * m);
      // 文字列形式で返す
      return String.format("%.3f", bmi);
    } catch (NumberFormatException e) {
      // If invalid input is provided, return an error
      return "Invalid data";
    }
  }

  /**
   * 入力データの検証を行います。
   *
   * @param height 身長
   * @param weight 体重
   * @return 入力が有効かどうか
   */
  public boolean validate(String height, String weight) {
    // Null or empty validation
    if (height == null || height.isEmpty() || weight == null || weight.isEmpty()) {
      return false;
    }

    try {
      // Check if both height and weight are valid numbers
      Double.parseDouble(height);
      Double.parseDouble(weight);
      return true;
    } catch (NumberFormatException e) {
      return false; // Invalid number format
    }
  }
}
