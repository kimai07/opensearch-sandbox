# OpenSearch Sandbox

OpenSearchの各種機能を検証・学習するためのサンドボックスプロジェクトです。

## 要件

- Java 25
- Maven 3.9+
- Docker / Docker Compose

## セットアップ

### 1. OpenSearch 環境の起動

```bash
docker compose up -d
```

OpenSearch が起動するまで少し待ちます。以下のコマンドで状態を確認できます：

```bash
curl http://localhost:9200/_cluster/health?pretty
```

### 2. ビルド

```bash
mvn clean compile
```

### 3. テスト実行

```bash
mvn test
```

### 4. コードフォーマット

```bash
mvn formatter:format
```

### 5. デモの実行

```bash
mvn exec:java
```

## プロジェクト構成

```
opensearch-sandbox/
├── .github/                         # GitHub Actions ワークフロー
├── docs/                            # 設計ドキュメント
├── src/
│   ├── main/
│   │   ├── java/com/kimai07/opensearch/
│   │   │   ├── config/              # 接続設定（Builder パターン）
│   │   │   ├── client/              # クライアント層（Factory, Search, Index）
│   │   │   ├── search/              # 検索サービス（全文検索, ベクトル検索）
│   │   │   ├── index/               # インデックス管理サービス
│   │   │   └── examples/            # デモ
│   │   └── resources/               # 設定ファイル（application.properties）
│   └── test/java/                   # テストコード（main/java と同一パッケージ構成）
├── pom.xml                          # Maven設定
├── compose.yml                      # OpenSearch Docker Compose
└── CLAUDE.md                        # Claude Code用ガイド
```

## 機能

### 全文検索・クエリ

- **Match Query**: 全文検索
- **Multi Match Query**: 複数フィールド検索
- **Bool Query**: 複合条件検索（must/should/mustNot）
- **Fuzzy Query**: あいまい検索（編集距離ベース）
- **Phrase Match Query**: フレーズ一致検索
- **Wildcard Query**: ワイルドカード検索
- **Highlight**: 検索結果のハイライト

### ベクトル検索（k-NN）

- k-NN用インデックスの作成
- ベクトルデータの一括登録（Bulk API）
- k-NN類似検索
- フィルタ付きk-NN検索

### インデックス管理

- インデックスの作成・削除・存在確認
- マッピング設定・取得
- インデックステンプレートの作成・削除
- インデックス設定の取得
- インデックスのリフレッシュ

## アクセス先

- **OpenSearch**: http://localhost:9200
- **OpenSearch Dashboards**: http://localhost:5601

## 停止

```bash
docker compose down
```

データも削除する場合：

```bash
docker compose down -v
```
