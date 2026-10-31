## CLAUDE.md
必ず日本語で回答してください。
This file provides guidance to Claude Code (claude,ai/code) when working with code in this repository.

## Project Overview

OpenSearchの各種機能を検証・学習するためのサンドボックスプロジェクト。

## Tech Stack

- **Language**: Java 25
- **Build Tool**: Maven
- **OpenSearch**: 3.9.0 (Docker Compose)
- **Client**: opensearch-java 3.10.0
- **API**: Spring Boot 4.1.2

## Development Commands

```bash
# OpenSearch環境の起動
docker compose up -d

# ビルド
mvn clean compile

# テスト実行
mvn test

# コードフォーマット
mvn formatter:format

# デモ実行
mvn -pl core compile exec:java

# 検索APIの起動
mvn -DskipTests package
java -jar api/target/opensearch-sandbox-api-1.0.0-SNAPSHOT.jar

# リグレッションテスト（OpenSearchの起動が必要）
docker compose up -d --wait opensearch
mvn verify -Pregression

# リグレッションテストの期待値を更新
mvn verify -Pregression -Dregression.update=true

# OpenSearch環境の停止
docker compose down
```

## Project Structure

`core`（OpenSearchの操作とデモ）と `api`（Spring Bootの検索API）のマルチモジュール構成。
依存の向きは `api → core` のみで、`core` はSpringに依存しない。

```
core/src/main/java/com/kimai07/opensearch/
├── OpenSearchSandboxApplication.java  # エントリーポイント
├── config/
│   └── OpenSearchConfig.java          # 接続設定（Builder パターン）
├── client/
│   ├── OpenSearchClientFactory.java   # クライアントファクトリ（Closeable）
│   ├── SearchClient.java             # 検索インターフェース
│   ├── OpenSearchSearchClient.java   # 検索実装
│   ├── IndexClient.java             # インデックスインターフェース
│   └── OpenSearchIndexClient.java   # インデックス実装
├── search/
│   ├── FullTextSearchService.java     # 全文検索
│   └── VectorSearchService.java       # ベクトル検索(k-NN)
├── index/
│   └── IndexManagementService.java    # インデックス管理（VectorDocument record含む）
└── examples/
    ├── DemoRunner.java                # デモランナー
    ├── FullTextSearchDemo.java        # 全文検索デモ
    ├── VectorSearchDemo.java          # ベクトル検索デモ
    └── SearchResultPrinter.java       # 検索結果表示ユーティリティ

core/src/test/java/com/kimai07/opensearch/
├── client/
│   └── OpenSearchClientFactoryTest.java
├── config/
│   └── OpenSearchConfigTest.java
├── search/
│   └── FullTextSearchServiceTest.java
└── index/
    └── IndexManagementServiceTest.java

api/src/main/java/com/kimai07/opensearch/api/
├── OpenSearchApiApplication.java      # エントリーポイント
├── config/                            # 接続設定、Bean定義
└── search/                            # 検索機能
    ├── controller/                    # GET /api/v1/articles/search
    ├── service/
    ├── repository/                    # interfaceとOpenSearch実装
    ├── model/
    ├── dto/
    └── error/

api/src/test/java/                     # 単体テスト
api/src/integration-test/
├── java/                              # 結合テスト（*IT）
└── resources/regression/              # テストケース、期待値、投入データ
```

検索対象のデータが増えたら、種類ごとに service と repository を追加する。
OpenSearch の型は repository の実装に閉じ込め、Jackson 2 の型を Web 層に出さない（`api` の Web 層は Jackson 3）。

## Key Features

1. **全文検索**: Match Query, Multi Match Query, Bool Query, Fuzzy Query, Phrase Match Query, Wildcard Query, Highlight
2. **ベクトル検索(k-NN)**: k-NN Index作成、ベクトル一括登録、類似検索、フィルタ付きk-NN検索
3. **インデックス管理**: 作成・削除、マッピング設定・取得、インデックステンプレート、リフレッシュ、設定取得
4. **検索API**: `GET /api/v1/articles/search`（OpenSearchの接続先は環境変数で切り替え）

## CI

ワークフローはすべて手動実行で、`main` へのpushやPRでは自動実行しない。

- `search-regression.yml`: 期待値のJSONと比較する（PR番号は任意、空の場合は`main`）
- `search-compare.yml`: `main`とPRのレスポンスを比較し、結果をPRのステータスに表示する

## Access URLs

- OpenSearch: http://localhost:9200
- OpenSearch Dashboards: http://localhost:5601
