package com.study.r4a122.webportal.chat;

import java.time.LocalDateTime;

public record MessageData(
    /** メッセージID : 主キー */
    int id,
    
    /** チャンネルID */
    int channelId,
    
    /** ユーザーID */
    String userId,
    
    /** ユーザー名 */
    String userName,
    
    /** メッセージテキスト */
    String messageText,
    
    /** 親メッセージID（スレッド用） */
    Integer parentMessageId,

    /** 重要度 */
    String importance,
    
    /** 編集済みフラグ */
    boolean isEdited,
    
    /** 削除済みフラグ */
    boolean isDeleted,
    
    /** 作成日時 */
    LocalDateTime createdAt,
    
    /** 更新日時 */
    LocalDateTime updatedAt) {
    
    // 後方互換性のためのコンストラクタ
    public MessageData(int id, int channelId, String userId, String userName, String messageText, LocalDateTime createdAt) {
        this(id, channelId, userId, userName, messageText, null, "NORMAL", false, false, createdAt, null);
    }
}

