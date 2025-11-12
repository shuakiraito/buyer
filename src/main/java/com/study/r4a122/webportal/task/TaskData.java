package com.study.r4a122.webportal.task;

import java.util.Date; // java.sql.Date から java.util.Date に変更

public record TaskData(
        /** タスクID : 主キー、SQLにて自動採番 */
        int id,

        /** ユーザーID (メールアドレス) : Userテーブルの主キーと紐づく、ログイン情報から取得 */
        String userId,

        /** 件名 : 必須入力 */
        String title,

        /** 期限日 : 必須入力 */
        Date limitday, // ← java.util.Date に変更

    /** 完了フラグ : デフォルト値は、false(未完了) */
    boolean completed) {
}
