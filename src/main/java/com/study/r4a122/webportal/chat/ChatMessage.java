package com.study.r4a122.webportal.chat;

public class ChatMessage {
  private Integer id;
  private int channelId;
  private String userId;
  private String userName;
  private String messageText;
  private String importance;
  private String eventType;
  private String createdAt;
  private String updatedAt;
  private boolean edited;
  private boolean deleted;

  public ChatMessage() {
  }

  public ChatMessage(int channelId, String userId, String userName, String messageText) {
    this.channelId = channelId;
    this.userId = userId;
    this.userName = userName;
    this.messageText = messageText;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public int getChannelId() {
    return channelId;
  }

  public void setChannelId(int channelId) {
    this.channelId = channelId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getMessageText() {
    return messageText;
  }

  public void setMessageText(String messageText) {
    this.messageText = messageText;
  }

  public String getImportance() {
    return importance;
  }

  public void setImportance(String importance) {
    this.importance = importance;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }

  public boolean isEdited() {
    return edited;
  }

  public void setEdited(boolean edited) {
    this.edited = edited;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }
}

