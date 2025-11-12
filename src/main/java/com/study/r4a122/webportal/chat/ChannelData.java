package com.study.r4a122.webportal.chat;

import java.time.LocalDateTime;

public record ChannelData(
    /** チャンネルID : 主キー */
    int id,
    
    /** チャンネル名 */
    String channelName,
    
    /** 説明 */
    String description,
    
    /** 公開フラグ */
    boolean isPublic,
    
    /** ステータス */
    String status,
    
    /** 作成日時 */
    LocalDateTime createdAt,
    
    /** 作成者 */
    String createdBy) {
    
    // 後方互換性のためのコンストラクタ
    public ChannelData(int id, String channelName, String description, LocalDateTime createdAt, String createdBy) {
        this(id, channelName, description, true, "ACTIVE", createdAt, createdBy);
    }
}

