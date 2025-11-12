package com.study.r4a122.webportal.task;

import java.util.ArrayList;
import java.util.List;

public record TaskEntity(
    /** タスク情報のリスト */
    List<TaskData> taskList,
    /** エラーメッセージ(表示用) */
    String errorMessage) {

  public TaskEntity() {
    this(new ArrayList<>(), "");
  }
}
