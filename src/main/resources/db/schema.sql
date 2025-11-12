/* テーブル削除：CREATE文と逆順で記載する */
DROP TABLE IF EXISTS remember_me_token_t;
DROP TABLE IF EXISTS channel_invitation_t;
DROP TABLE IF EXISTS message_file_t;
DROP TABLE IF EXISTS channel_star_t;
DROP TABLE IF EXISTS activity_t;
DROP TABLE IF EXISTS direct_message_t;
DROP TABLE IF EXISTS message_read_t;
DROP TABLE IF EXISTS reaction_t;
DROP TABLE IF EXISTS thread_t;
DROP TABLE IF EXISTS message_t;
DROP TABLE IF EXISTS channel_member_t;
DROP TABLE IF EXISTS channel_request_t;
DROP TABLE IF EXISTS channel_t;
DROP TABLE IF EXISTS task_t;
DROP TABLE IF EXISTS user_m;

/* ユーザーマスタ */
CREATE TABLE
  user_m (
    user_id VARCHAR(50) PRIMARY KEY,
    PASSWORD VARCHAR(100),
    user_name VARCHAR(50),
    ROLE VARCHAR(50),
    enabled BOOLEAN,
    failed_attempts INT DEFAULT 0,
    locked BOOLEAN DEFAULT false,
    locked_at TIMESTAMP,
    last_login_at TIMESTAMP,
    remember_token VARCHAR(255),
    remember_token_expires TIMESTAMP
  );

/* タスクテーブル */
CREATE TABLE
  task_t (
    id INT PRIMARY KEY,
    user_id VARCHAR(50),
    title VARCHAR(50),
    limitday DATE,
    complete BOOLEAN
  );

/* チャンネルテーブル */
CREATE TABLE
  channel_t (
    id INT PRIMARY KEY,
    channel_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_public BOOLEAN DEFAULT true,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50)
  );

/* チャンネル申請テーブル */
CREATE TABLE
  channel_request_t (
    id INT PRIMARY KEY,
    channel_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_public BOOLEAN DEFAULT true,
    requested_by VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_by VARCHAR(50),
    reviewed_at TIMESTAMP
  );

/* チャンネルメンバーテーブル */
CREATE TABLE
  channel_member_t (
    id INT PRIMARY KEY,
    channel_id INT NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (channel_id) REFERENCES channel_t(id) ON DELETE CASCADE,
    UNIQUE(channel_id, user_id)
  );

/* メッセージテーブル */
CREATE TABLE
  message_t (
    id INT PRIMARY KEY,
    channel_id INT NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    message_text TEXT NOT NULL,
    parent_message_id INT,
    importance VARCHAR(10) DEFAULT 'NORMAL',
    is_edited BOOLEAN DEFAULT false,
    is_deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (channel_id) REFERENCES channel_t(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_message_id) REFERENCES message_t(id) ON DELETE SET NULL
  );

/* スレッドテーブル（メッセージへの返信用） */
CREATE TABLE
  thread_t (
    id INT PRIMARY KEY,
    message_id INT NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    thread_text TEXT NOT NULL,
    is_edited BOOLEAN DEFAULT false,
    is_deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES message_t(id) ON DELETE CASCADE
  );

/* リアクションテーブル */
CREATE TABLE
  reaction_t (
    id INT PRIMARY KEY,
    message_id INT,
    thread_id INT,
    user_id VARCHAR(50) NOT NULL,
    emoji VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES message_t(id) ON DELETE CASCADE,
    FOREIGN KEY (thread_id) REFERENCES thread_t(id) ON DELETE CASCADE,
    CHECK ((message_id IS NOT NULL AND thread_id IS NULL) OR (message_id IS NULL AND thread_id IS NOT NULL))
  );

/* メッセージ未読管理テーブル */
CREATE TABLE
  message_read_t (
    id INT PRIMARY KEY,
    message_id INT NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES message_t(id) ON DELETE CASCADE,
    UNIQUE(message_id, user_id)
  );

/* ダイレクトメッセージテーブル */
CREATE TABLE
  direct_message_t (
    id INT PRIMARY KEY,
    sender_id VARCHAR(50) NOT NULL,
    receiver_id VARCHAR(50) NOT NULL,
    message_text TEXT NOT NULL,
    is_edited BOOLEAN DEFAULT false,
    is_deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES user_m(user_id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES user_m(user_id) ON DELETE CASCADE
  );

/* アクティビティテーブル */
CREATE TABLE
  activity_t (
    id INT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    activity_message TEXT NOT NULL,
    related_id INT,
    related_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user_m(user_id) ON DELETE CASCADE
  );

/* チャンネルスターテーブル */
CREATE TABLE
  channel_star_t (
    id INT PRIMARY KEY,
    channel_id INT NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (channel_id) REFERENCES channel_t(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user_m(user_id) ON DELETE CASCADE,
    UNIQUE(channel_id, user_id)
  );

/* メッセージファイルテーブル */
CREATE TABLE
  message_file_t (
    id INT PRIMARY KEY,
    message_id INT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(100),
    uploaded_by VARCHAR(50) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES message_t(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES user_m(user_id) ON DELETE CASCADE
  );

/* チャンネル招待テーブル */
CREATE TABLE
  channel_invitation_t (
    id INT PRIMARY KEY,
    channel_id INT NOT NULL,
    inviter_id VARCHAR(50) NOT NULL,
    invitee_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP,
    FOREIGN KEY (channel_id) REFERENCES channel_t(id) ON DELETE CASCADE,
    FOREIGN KEY (inviter_id) REFERENCES user_m(user_id) ON DELETE CASCADE,
    FOREIGN KEY (invitee_id) REFERENCES user_m(user_id) ON DELETE CASCADE,
    UNIQUE(channel_id, invitee_id)
  );

/* ログイン保持トークンテーブル */
CREATE TABLE
  remember_me_token_t (
    id INT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    revoked BOOLEAN DEFAULT false,
    FOREIGN KEY (user_id) REFERENCES user_m(user_id) ON DELETE CASCADE
  );
