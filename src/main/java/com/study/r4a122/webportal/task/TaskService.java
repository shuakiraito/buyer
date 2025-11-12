package com.study.r4a122.webportal.task;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

  @Autowired
  private TaskRepository taskRepository;

  public List<TaskData> selectAll(String userId) {
    TaskEntity taskEntity = taskRepository.findTasksByUserId(userId);
    return taskEntity != null ? taskEntity.taskList() : List.of();
  }

  public boolean insert(String userId, String title, String limitday) {
    if (!validate(title, limitday))
      return false;

    try {
      Date ud = new SimpleDateFormat("yyyy-MM-dd").parse(limitday);
      TaskData td = new TaskData(0, userId, title, ud, false);
      taskRepository.save(td); // SQLException を投げるため catch で処理
      return true;
    } catch (ParseException | SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean validate(String title, String limitday) {
    if (title == null || title.isBlank() || title.length() > 50)
      return false;
    if (limitday == null || limitday.isBlank())
      return false;

    try {
      new SimpleDateFormat("yyyy-MM-dd").parse(limitday);
    } catch (ParseException e) {
      return false;
    }
    return true;
  }

  public boolean completeTask(String userId, Long taskId) {
    TaskEntity taskEntity = taskRepository.findTasksByUserId(userId);
    if (taskEntity == null)
      return false;

    // taskIdはintにキャストしてTaskDataのidと比較
    int idInt = taskId.intValue();
    boolean found = false;

    for (TaskData td : taskEntity.taskList()) {
      if (td.id() == idInt) {
        found = true;
        break;
      }
    }
    if (!found)
      return false;

    try {
      taskRepository.update(idInt);
      return true;
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean delete(String userId, String id) {
    int taskId;
    try {
      taskId = Integer.parseInt(id);
    } catch (NumberFormatException e) {
      return false;
    }

    TaskEntity taskEntity = taskRepository.findTasksByUserId(userId);
    if (taskEntity == null)
      return false;

    boolean found = taskEntity.taskList().stream()
        .anyMatch(td -> td.id() == taskId);

    if (!found)
      return false;

    try {
      taskRepository.delete(taskId);
      return true;
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }
}
