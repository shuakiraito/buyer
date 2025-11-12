/* データ削除：INSERT文と逆順で記載する */
DELETE FROM user_m;

/* データ削除：INSERT文と逆順で記載する */
DELETE FROM message_t;
DELETE FROM channel_t;
DELETE FROM task_t;

/* タスクテーブルのデータ */
INSERT INTO
  task_t (id, user_id, title, limitday, complete)
VALUES
  (1, 'admin', 'このレコードは消さないこと', '2022-11-11', TRUE);

INSERT INTO
  task_t (id, user_id, title, limitday, complete)
VALUES
  (
    2,
    'goro@xxx.co.jp',
    '食材を購入するためのリストを作成',
    '2023-06-24',
    FALSE
  );

INSERT INTO
  task_t (id, user_id, title, limitday, complete)
VALUES
  (
    3,
    'hanako@xxx
    .co.jp',
    '明日の夕食にレストランを予約しているので、確認の電話をかける',
    '2023-07-11',
    FALSE
  );

INSERT INTO
  task_t (id, user_id, title, limitday, complete)
VALUES
  (
    4,
    'taro@xxx.co.jp',
    '近所のスポーツクラブに登録するため、必要な書類と登録費用を準備する',
    '2024-02-27',
    FALSE
  );

/* チャンネルテーブルのデータ */
INSERT INTO
  channel_t (id, channel_name, description, created_by)
VALUES
  (1, 'general', '一般的な会話', 'taro@xxx.co.jp');

INSERT INTO
  channel_t (id, channel_name, description, created_by)
VALUES
  (2, 'random', '雑談', 'hanako@xxx.co.jp');
