package com.study.r4a122.webportal.chat;

import java.time.LocalDateTime;

public record MessageFileData(
    int id,
    int messageId,
    String fileName,
    String filePath,
    Long fileSize,
    String fileType,
    String uploadedBy,
    LocalDateTime uploadedAt) {
}

