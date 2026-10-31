# OpenSearch Sandbox 要件定義書

## 1. プロジェクト概要

OpenSearchの各種機能を検証・学習するためのサンドボックスプロジェクト。

## 2. 技術スタック

- **言語**: Java 25
- **ビルドツール**: Maven
- **OpenSearch**: 3.9.0（Docker Compose で起動）
- **クライアント**: opensearch-java 3.10.0
- **API**: Spring Boot 4.1.2

## 3. 機能要件

### 3.1 全文検索・クエリ機能
- Match Query による全文検索
- Multi Match Query による複数フィールド検索
- Bool Query による複合条件検索（must/should/mustNot）
- Fuzzy Query によるあいまい検索（編集距離ベース）
- Phrase Match Query によるフレーズ一致検索
- Wildcard Query によるワイルドカード検索
- Highlight による検索結果ハイライト

### 3.2 ベクトル検索（k-NN）機能
- k-NN 用インデックスの作成
- ベクトルデータの一括登録（Bulk API）
- k-NN による類似検索
- フィルタ付き k-NN 検索

### 3.3 インデックス管理機能
- インデックスの作成・削除・存在確認
- マッピング定義・取得
- インデックステンプレートの作成・削除
- インデックス設定の取得
- インデックスのリフレッシュ

### 3.4 検索 API
- タイトルと本文を対象にした記事の全文検索
- ハイライト付きのレスポンス（本文の HTML はエスケープする）
- OpenSearch の接続先を環境変数で切り替え

## 4. 非機能要件

### 4.1 開発環境
- Docker Compose による OpenSearch 環境構築
- 単一ノード構成（開発用）

### 4.2 テスト
- JUnit 6 + Mockito + AssertJ によるユニットテスト
- 検索 API のレスポンスを期待値と比較するリグレッションテスト（Docker の OpenSearch に軽量データを投入して実行）

## 5. 想定バージョン

| コンポーネント         | バージョン  |
|-----------------|--------|
| Java            | 25     |
| OpenSearch      | 3.9.0  |
| opensearch-java | 3.10.0 |
| httpclient5     | 5.6.5  |
| Jackson         | 2.22.4 |
| SLF4J           | 2.0.20 |
| Logback         | 1.6.6  |
| JUnit           | 6.1.3  |
| AssertJ         | 3.27.7 |
| Mockito         | 5.24.0 |
| byte-buddy      | 1.18.16 |
| Maven           | 3.9+   |

`api` モジュールでは Spring Boot 4.1.2 の管理バージョンを使います。Jackson 2 は `opensearch-java` に合わせて 2.22.4 に固定し、Web 層は Jackson 3 を使います。

## 6. API 仕様

### 検索 API

```
GET /api/v1/articles/search?q=OpenSearch&size=10
```

| パラメータ | 必須 | 内容 |
|---|---|---|
| `q` | はい | 検索文字列。`title` と `content` を検索する |
| `size` | いいえ | 取得する最大件数（1〜100、既定値は 10） |

スコアの降順で並べ、同点の場合は `article_id` の昇順で並べます。

| ステータス | 条件 |
|---|---|
| 200 | 成功 |
| 400 | `q` が空、`size` が範囲外または数値でない |
| 503 | OpenSearch に接続できない、エラーが返った |

### 6.1 IndexManagementService

| メソッド | 説明 |
|--------|------|
| `createIndex(String indexName)` | デフォルト設定でインデックスを作成 |
| `createIndex(String indexName, Map<String, Property> properties)` | マッピング指定でインデックスを作成 |
| `createIndex(String indexName, Map<String, Property> properties, boolean enableKnn)` | k-NN有効化オプション付きでインデックスを作成 |
| `deleteIndex(String indexName)` | インデックスを削除 |
| `indexExists(String indexName)` | インデックスの存在確認 |
| `putMapping(String indexName, Map<String, Property> properties)` | マッピングを追加・更新 |
| `putIndexTemplate(String templateName, String indexPattern, Map<String, Property> properties)` | インデックステンプレートを作成 |
| `deleteIndexTemplate(String templateName)` | インデックステンプレートを削除 |
| `getIndexSettings(String indexName)` | インデックス設定を取得 |
| `getIndexMapping(String indexName)` | インデックスマッピングを取得 |
| `refreshIndex(String indexName)` | インデックスをリフレッシュ |
| `bulkIndexVectors(String indexName, String vectorField, List<VectorDocument> documents)` | ベクトルを一括登録 |

### 6.2 FullTextSearchService

| メソッド | 説明 |
|--------|------|
| `matchQuery(String indexName, String field, String query, Class<T> clazz)` | Match Queryで検索 |
| `multiMatchQuery(String indexName, List<String> fields, String query, Class<T> clazz)` | 複数フィールドに対するMulti Match Query |
| `boolQuery(String indexName, List<Query> must, List<Query> should, List<Query> mustNot, Class<T> clazz)` | Bool Queryで検索 |
| `fuzzyQuery(String indexName, String field, String value, String fuzziness, Class<T> clazz)` | Fuzzy Queryで検索 |
| `phraseMatchQuery(String indexName, String field, String phrase, Class<T> clazz)` | フレーズ一致検索 |
| `wildcardQuery(String indexName, String field, String pattern, Class<T> clazz)` | ワイルドカード検索 |
| `searchWithHighlight(String indexName, String field, String query, Class<T> clazz)` | ハイライト付きで検索 |
| `multiMatchWithHighlight(String indexName, List<String> fields, String query, int size, String tieBreakerField, Class<T> clazz)` | 複数フィールドをハイライト付きで検索。同点時の順序を固定する |
| `extractDocuments(SearchResponse<T> response)` | レスポンスからドキュメントを抽出 |
| `extractHighlights(Hit<T> hit)` | ヒットからハイライト情報を抽出 |

### 6.3 VectorSearchService

| メソッド | 説明 |
|--------|------|
| `knnSearch(String indexName, String vectorField, float[] queryVector, int k, Class<T> clazz)` | k-NN検索を実行 |
| `knnSearchWithFilter(String indexName, String vectorField, float[] queryVector, int k, Query filter, Class<T> clazz)` | フィルタ付きk-NN検索 |
