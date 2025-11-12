package com.study.r4a122.webportal.zip;

import java.util.regex.Pattern;

public class ZipEntity {

  private String zipcode;

  // 郵便番号の正規表現: 7桁の数字（ハイフンなし）
  private static final Pattern ZIPCODE_PATTERN = Pattern.compile("^\\d{7}$");

  public ZipEntity(String zipcode) {
    validate(zipcode);
    this.zipcode = zipcode;
  }

  private void validate(String zipcode) {
    if (zipcode == null || zipcode.trim().isEmpty()) {
      throw new IllegalArgumentException("郵便番号が未入力です。");
    }

    if (!ZIPCODE_PATTERN.matcher(zipcode).matches()) {
      throw new IllegalArgumentException("郵便番号は7桁の数字で入力してください。");
    }

    // 特殊文字チェック（数字以外が含まれていた場合）
    if (!zipcode.matches("\\d+")) {
      throw new IllegalArgumentException("郵便番号には数字以外の文字が含まれています。");
    }
  }

  public String getZipcode() {
    return zipcode;
  }

  public void setZipcode(String zipcode) {
    validate(zipcode);
    this.zipcode = zipcode;
  }
}
