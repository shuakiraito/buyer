# Web アプリケーション構築 実習ワークスペース

## VSCode に最初にインストールする 拡張機能

### 入れ方

1. 左の「拡張機能」ボタンを押す
1. 検索ボックスに「@recommended」を入れる
1. 「ワークスペースの推奨事項」に表示されている Extension をインストール

## PostgreSQL DB への接続

### PostgreSQL 拡張機能の活用

1. VSCode 左側のアイコンの中から「Database」を開く

2. 「Create Connection」を押す

3. 「Connect」内で以下の設定を入力し、下部の「＋ Connect」ボタンを押す

- Name : DB
- Host : 127.0.0.1 ※デフォルト
- Port : 5432 ※デフォルト
- Username : postgres ※デフォルト
- Password : postgres
- Database : postgres ※デフォルト

4. PostgreSQL の DB に接続されれば準備完了

- ※エラーになった場合は、PostgreSQL のインストール状況を確認する
