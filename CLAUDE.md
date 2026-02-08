## CLAUDE.md
必ず日本語で回答してください。
This file provides guidance to Claude Code (claude,ai/code) when working with code in this repository.

## Project Overview

OpenSearchの各種機能を検証・学習するためのサンドボックスプロジェクト。

## Tech Stack

- **Language**: Java 25
- **Build Tool**: Maven
- **OpenSearch**: 3.3.2 (Docker Compose)
- **Client**: opensearch-java 3.3.0

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
mvn exec:java

# OpenSearch環境の停止
docker compose down
```

## Project Structure

```
src/main/java/com/kimai07/opensearch/
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

src/test/java/com/kimai07/opensearch/
├── client/
│   └── OpenSearchClientFactoryTest.java
├── config/
│   └── OpenSearchConfigTest.java
├── search/
│   └── FullTextSearchServiceTest.java
└── index/
    └── IndexManagementServiceTest.java
```

## Key Features

1. **全文検索**: Match Query, Multi Match Query, Bool Query, Fuzzy Query, Phrase Match Query, Wildcard Query, Highlight
2. **ベクトル検索(k-NN)**: k-NN Index作成、ベクトル一括登録、類似検索、フィルタ付きk-NN検索
3. **インデックス管理**: 作成・削除、マッピング設定・取得、インデックステンプレート、リフレッシュ、設定取得

## Access URLs

- OpenSearch: http://localhost:9200
- OpenSearch Dashboards: http://localhost:5601
