# アーキテクチャ図

## システム構成図

```mermaid
flowchart TB
    subgraph Client["クライアントアプリケーション"]
        App[OpenSearchSandboxApplication]
        Demo[DemoRunner]
    end

    subgraph Services["サービス層"]
        FTS[FullTextSearchService]
        VS[VectorSearchService]
        IMS[IndexManagementService]
    end

    subgraph ClientLayer["クライアント層"]
        Factory[OpenSearchClientFactory]
        SC[SearchClient]
        OSC[OpenSearchSearchClient]
        IC[IndexClient]
        OIC[OpenSearchIndexClient]
    end

    subgraph Config["設定"]
        Conf[OpenSearchConfig]
        Props[application.properties]
    end

    subgraph Infrastructure["インフラストラクチャ"]
        subgraph Docker["Docker Compose"]
            OS[(OpenSearch<br/>:9200)]
            Dashboard[OpenSearch Dashboards<br/>:5601]
        end
    end

    App --> Demo
    Demo --> FTS
    Demo --> VS
    Demo --> IMS

    FTS --> SC
    SC -.->|interface| OSC
    OSC --> Factory
    VS --> Factory
    IMS --> IC
    IC -.->|interface| OIC
    OIC --> Factory

    Factory --> Conf
    Conf --> Props
    Factory -->|HTTP| OS
    Dashboard -->|内部接続| OS
```

## レイヤー構成

```mermaid
flowchart LR
    subgraph Presentation["プレゼンテーション層"]
        direction TB
        A1[OpenSearchSandboxApplication]
        A2[DemoRunner]
    end

    subgraph Business["ビジネスロジック層"]
        direction TB
        B1[FullTextSearchService]
        B2[VectorSearchService]
        B3[IndexManagementService]
    end

    subgraph Data["データアクセス層"]
        direction TB
        C1[OpenSearchClientFactory]
        C2[SearchClient]
        C3[OpenSearchSearchClient]
        C4[IndexClient]
        C5[OpenSearchIndexClient]
    end

    subgraph External["外部システム"]
        direction TB
        D1[(OpenSearch)]
    end

    Presentation --> Business
    Business --> Data
    Data --> External
```

## パッケージ構成

```
com.kimai07.opensearch/
├── OpenSearchSandboxApplication.java   # エントリーポイント
│
├── config/                             # 設定
│   └── OpenSearchConfig.java           # 接続設定（Builder パターン）
│
├── client/                             # クライアント
│   ├── OpenSearchClientFactory.java    # クライアントファクトリ（シングルトン、Closeable）
│   ├── SearchClient.java               # 検索インターフェース
│   ├── OpenSearchSearchClient.java     # 検索実装
│   ├── IndexClient.java                # インデックスインターフェース
│   └── OpenSearchIndexClient.java      # インデックス実装
│
├── search/                             # 検索サービス
│   ├── FullTextSearchService.java      # 全文検索
│   └── VectorSearchService.java        # ベクトル検索（k-NN）
│
├── index/                              # インデックス管理
│   └── IndexManagementService.java     # インデックス CRUD
│
└── examples/                           # デモ
    ├── DemoRunner.java                 # デモランナー
    ├── FullTextSearchDemo.java         # 全文検索デモ
    ├── VectorSearchDemo.java           # ベクトル検索デモ
    └── SearchResultPrinter.java        # 検索結果表示ユーティリティ
```

## データフロー

```mermaid
sequenceDiagram
    participant App as Application
    participant Demo as DemoRunner
    participant Service as SearchService
    participant Factory as ClientFactory
    participant OS as OpenSearch

    App->>Demo: main()
    Demo->>Factory: create()
    Factory->>Factory: OpenSearchConfig.fromProperties()
    Demo->>Factory: testConnection()
    Factory->>OS: GET /
    OS-->>Factory: cluster info

    Note over Demo: 全文検索デモ
    Demo->>Service: matchQuery()
    Service->>Factory: getClient()
    Factory-->>Service: OpenSearchClient
    Service->>OS: POST /_search
    OS-->>Service: SearchResponse
    Service-->>Demo: 検索結果

    Note over Demo: ベクトル検索デモ
    Demo->>Service: knnSearch()
    Service->>OS: POST /_search (k-NN)
    OS-->>Service: SearchResponse
    Service-->>Demo: 類似ベクトル

    Demo->>Factory: close()
```

## 主要機能

| 機能カテゴリ | クラス | 主要メソッド |
|------------|--------|------------|
| 全文検索 | FullTextSearchService | matchQuery, multiMatchQuery, boolQuery, fuzzyQuery, phraseMatchQuery, wildcardQuery, searchWithHighlight |
| ベクトル検索 | VectorSearchService | knnSearch, knnSearchWithFilter |
| インデックス管理 | IndexManagementService | createIndex, deleteIndex, putMapping, putIndexTemplate, refreshIndex, bulkIndexVectors |