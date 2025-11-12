# チャット機能拡張 設計メモ

## 目的
- Slack / Google Chat を参考に、既存のチャットモジュールへグループ運用・検索・権限制御・アカウント管理を包括的に拡張する。
- 追加要件の大半は既存データ構造の拡充と UI・リアルタイム連携の強化で実現する。

## スコープ整理
- **チャット**: グループ / DM / メッセージ編集・削除 / リアクション / WebSocket 双方向通信。
- **グループ**: 作成・更新・削除・公開範囲設定・招待・参加・除名。
- **検索**: キーワード / 投稿者 / 日付 / 重要度。
- **ログイン**: Remember-Me / 失敗回数によるロック。
- **アカウント管理**: 作成・変更・ロック解除・強制退会。
- **見逃し防止**: 重要度設定・未読者抽出・リマインド・バッジ。
- **権限**: 一般ユーザー (ROLE_USER)、講師/モデレーター (ROLE_MANAGER)、管理者 (ROLE_ADMIN) を想定。

## データモデル変更
- `user_m`
  - `failed_attempts INT DEFAULT 0`
  - `locked BOOLEAN DEFAULT false`
  - `locked_at TIMESTAMP`
  - `last_login_at TIMESTAMP`
  - `remember_token VARCHAR(255)` / `remember_token_expires TIMESTAMP`
- `channel_t`
  - `is_public` を利用継続
  - `channel_name` のユニーク制約 (論理)
- `message_t`
  - `importance VARCHAR(10) DEFAULT 'NORMAL'` (NORMAL / IMPORTANT / URGENT)
  - `remind_after_minutes INT DEFAULT NULL` (将来拡張用)
- `message_read_t`
  - 現行構造を流用し集計 API を追加
- `direct_message_t`
  - 編集・削除フラグを既存利用、更新系 SQL を追加
- 新規テーブル
  - `channel_invitation_t (id, channel_id, inviter_id, invitee_id, status, created_at, responded_at)`
  - `login_token_t (id, user_id, token_hash, issued_at, expires_at, revoked BOOLEAN)`

## バックエンド変更
- `ChatService`
  - メッセージ保存・更新に `importance` を受け渡し。
  - 管理者が全メッセージ編集/削除できるよう権限判定追加。
  - DM 用 `updateDirectMessage`, `deleteDirectMessage` を新設。
  - 重要度検索 / 未読集計 / バッジ取得 API を拡張。
- `MessageRepository`, `ThreadRepository`
  - 更新・削除 SQL を `role` に応じて切替 (service 層で判定、repository は `updateById`, `deleteById` を追加)。
  - 検索クエリに `importance` フィルター列追加。
- `ChannelRepository`
  - `updateMetadata(channelId, name, description, isPublic)` と `softDelete(channelId)` を追加。
- `ChannelMemberRepository`
  - `removeMember(channelId, userId)` と `listMembersWithProfile` を追加。
- `ChannelInvitationRepository`
  - 招待作成 / 承認 / 拒否管理。
- `RememberMeService`
  - ログイン時トークン発行、Cookie 設定、検証。
- `LoginService`
  - 失敗回数管理とロック制御、成功時リセット。
- `AccountService`
  - ユーザー一覧 / 更新 / ロック解除 / 強制退会を司る。

## コントローラー変更
- `ChatController`
  - メッセージ POST 時に重要度・ファイル・リアクションをまとめて送信。
  - グループ設定 (公開/非公開、メンバー招待、一括追加) の新規エンドポイント。
  - 未読バッジ / 重要メッセージ一覧 API。
  - DM 編集・削除 API。
- `LoginController`
  - `rememberMe` チェックボックス対応。
  - ロック状態のフィードバック。
- 新規 `AccountController`
  - 管理者 UI からの CRUD・ロック解除・強制退会。
- 新規 `ChannelInvitationController`
  - 招待承認／拒否、招待一覧表示。

## フロントエンド (Thymeleaf + Vanilla JS)
- `chat/index.html`
  - チャンネル一覧にスター・未読バッジ・公開/非公開アイコン追加。
  - チャンネル作成モーダル (公開設定・初期メンバー選択)。
- `chat/channel.html`
  - メッセージ重要度バッジ、リアクションパネル、編集/削除操作の権限制御。
  - WebSocket イベントを重要度・リアクション等に対応。
  - 未読者表示 / リマインド操作 (管理者限定)。
- `chat/dm*.html`
  - DM 編集・削除、リアルタイム更新 (WebSocket `/topic/dm.{conversationId}`)。
- `chat/search.html`
  - 重要度 / 投稿者 / 日付 / グループでフィルタリング。
- 新規 `chat/group-settings.html`
  - 招待状況・メンバー管理 UI。
- `login.html`
  - Remember-Me チェックボックス、ロック時メッセージ表示。
- `account/*.html`
  - アカウント一覧・編集モーダル。

## リアルタイム要件
- 既存 `/topic/messages` にチャンネル別サブトピック (`/topic/channel.{id}`) を追加。
- DM 用 `/topic/dm.{pairKey}` を追加 (送信者・受信者両方へ push)。
- 重要度変更・削除時も同一トピックで通知。

## 権限制御
- `ROLE_USER`
  - 自分のメッセージ/DM の編集・削除のみ。
  - 自分が作成したグループの編集・削除。
- `ROLE_MANAGER`
  - 担当グループ (created_by) の管理全般。
  - 未読者抽出・リマインド。
- `ROLE_ADMIN`
  - 全グループ・全メッセージへのフルアクセス。
  - アカウントロック解除・強制退会。

## テスト戦略
- リポジトリ単体テスト: 主要 SQL の CRUD。
- サービス統合テスト: 権限判定・重要度検索・未読バッジ計算。
- Web 層テスト: コントローラ権限ガード、Remember-Me フロー。
- E2E (Selenium 省略可): チャンネル作成→招待→メッセージ投稿→検索。

## 今後の実装順序
1. DB スキーマおよびデータクラス更新。
2. サービス/リポジトリの新メソッド追加。
3. コントローラと WebSocket 更新。
4. テンプレート・フロント JS 改修。
5. テスト・Seed データ更新。
6. ドキュメント更新。


