package com.study.r4a122.webportal.task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.lang.NonNull;

import com.study.r4a122.webportal.WebConfig;

public class TaskRowCallbackHandler implements RowCallbackHandler {

  private final List<String> lines = new ArrayList<>();

  @Override
  public void processRow(@NonNull ResultSet rs) throws SQLException {
    String line = String.format(
        "%d,%s,%s,%s",
        rs.getInt("id"),
        escapeCsv(rs.getString("user_id")),
        escapeCsv(rs.getString("title")),
        rs.getDate("limitday"));
    lines.add(line);
  }

  public void writeToFile() throws SQLException {
    try {
      File directory = new File(WebConfig.OUTPUT_PATH);
      if (!directory.exists() && !directory.mkdirs()) {
        throw new IOException("出力ディレクトリの作成に失敗しました: " + WebConfig.OUTPUT_PATH);
      }

      // フルパスをディレクトリ + ファイル名で作成
      File file = new File(WebConfig.OUTPUT_PATH, WebConfig.FILENAME_TASK_CSV);

      try (
          FileWriter fw = new FileWriter(file, false);
          BufferedWriter writer = new BufferedWriter(fw)) {
        writer.write("id,user_id,title,limitday");
        writer.newLine();

        for (String line : lines) {
          writer.write(line);
          writer.newLine();
        }
      }

    } catch (IOException e) {
      throw new SQLException("CSVファイル出力中にエラーが発生しました", e);
    }
  }

  private String escapeCsv(String value) {
    if (value == null)
      return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
