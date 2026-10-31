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
mvn -pl core compile exec:java
```

### 6. 検索 API の起動

```bash
mvn -DskipTests package
java -jar api/target/opensearch-sandbox-api-1.0.0-SNAPSHOT.jar
```

```bash
curl "http://localhost:8080/api/v1/articles/search?q=OpenSearch&size=10"
```

OpenSearch の接続先は環境変数で指定します。

| 環境変数 | 既定値 |
|---|---|
| `OPENSEARCH_HOST` | `localhost` |
| `OPENSEARCH_PORT` | `9200` |
| `OPENSEARCH_SCHEME` | `http` |
| `OPENSEARCH_CONNECTION_TIMEOUT` | `5000` |
| `OPENSEARCH_SOCKET_TIMEOUT` | `60000` |
| `OPENSEARCH_INDEX` | `articles` |

### 7. リグレッションテスト

検索 API のレスポンスを期待値と比較します。OpenSearch に軽量データを投入して実行するので、インデックスはテストが作成します。

```bash
docker compose up -d --wait opensearch
mvn verify -Pregression
docker compose down -v
```

結合テストのコードは `api/src/integration-test/java/`、テストケースは `api/src/integration-test/resources/regression/` で管理します。

| ファイル | 内容 |
|---|---|
| `cases.json` | テストケース（ケース名とリクエスト） |
| `golden/<ケース名>.json` | 期待するステータスとボディ |
| `articles.ndjson` | 投入データ |
| `mapping.json` | インデックス定義 |

ケースを追加したときや、レスポンスを意図して変更したときは、期待値を更新して差分をレビューします。

```bash
mvn verify -Pregression -Dregression.update=true
```

### 8. GitHub Actions でのテスト

どちらのワークフローも手動で実行します。Actions 画面の「Run workflow」か、`gh` から実行できます。PR 番号を指定すると、ワークフロー内で最新の `main` に PR をマージした状態をテストします。実際の `main` は変更しません。

結果は実行画面の Job Summary に、ケースごとの表と `9件中 9件一致、0件不一致` の形式で表示します。`search-compare` の不一致は ❌ で示し、差分は Job Summary と、アーティファクト `search-compare-results` の `<ケース名>.diff` で確認できます。PR のコメントは 1 件だけで、実行のたびに最新の結果で更新します。

| ワークフロー | 内容 | PR 番号 |
|---|---|---|
| `search-regression.yml` | 期待値の JSON と比較する | 任意（空の場合は `main`） |
| `search-compare.yml` | `main` と PR のレスポンスを比較し、結果を PR のステータス `search-compare` とコメントに表示する | 必須 |

```bash
gh workflow run search-regression.yml -f pr_number=123
gh workflow run search-compare.yml -f pr_number=123
```

`search-compare` が失敗した場合は、差分を確認してください。原則マージせず、仕様変更と判断した場合のみ人がマージします。

Dependabot は週 1 回、更新の PR を作成します。

| PR の種類 | 確認方法 |
|---|---|
| Maven | `search-compare` |
| Docker イメージ | `search-regression` |
| GitHub Actions | 差分のレビューと、`--ref` で PR のブランチを指定した手動実行 |

## プロジェクト構成

```
opensearch-sandbox/
├── .github/                         # GitHub Actions ワークフロー
├── docs/                            # 設計ドキュメント
├── core/                            # OpenSearch の操作とデモ
│   └── src/main/java/com/kimai07/opensearch/
│       ├── config/                  # 接続設定（Builder パターン）
│       ├── client/                  # クライアント層（Factory, Search, Index）
│       ├── search/                  # 検索サービス（全文検索, ベクトル検索）
│       ├── index/                   # インデックス管理サービス
│       └── examples/                # デモ
├── api/                             # Spring Boot の検索 API
│   └── src/main/java/com/kimai07/opensearch/api/
│       ├── config/                  # 接続設定、Bean 定義
│       └── search/                  # 検索機能（controller, service, repository など）
├── pom.xml                          # 親 POM
├── compose.yml                      # OpenSearch Docker Compose
└── CLAUDE.md                        # Claude Code用ガイド
```

## 機能

### 検索 API

- **記事検索**: `GET /api/v1/articles/search`（タイトルと本文の全文検索、ハイライト付き）

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
- **検索 API**: http://localhost:8080

## 停止

```bash
docker compose down
```

データも削除する場合：

```bash
docker compose down -v
```
