package com.kimai07.opensearch.index;

import com.kimai07.opensearch.client.IndexClient;
import com.kimai07.opensearch.client.OpenSearchClientFactory;
import com.kimai07.opensearch.client.OpenSearchIndexClient;
import com.kimai07.opensearch.config.OpenSearchConfig;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.indices.GetIndicesSettingsResponse;
import org.opensearch.client.opensearch.indices.GetMappingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenSearchのインデックス管理サービス。
 * <p>
 * インデックスの作成、削除、マッピング設定、k-NNインデックスの作成、 インデックステンプレートの管理などの機能を提供します。
 * </p>
 *
 * @see OpenSearchClientFactory
 */
public class IndexManagementService {

    private static final Logger logger = LoggerFactory.getLogger(IndexManagementService.class);

    private final IndexClient client;
    private final OpenSearchClient openSearchClient;
    private final OpenSearchConfig config;

    /**
     * IndexManagementServiceを構築する。
     *
     * @param factory
     *            OpenSearch クライアントファクトリ
     */
    public IndexManagementService(OpenSearchClientFactory factory) {
        this.client = new OpenSearchIndexClient(factory.getClient());
        this.openSearchClient = factory.getClient();
        this.config = factory.getConfig();
    }

    /**
     * テスト用コンストラクタ。IndexClientとConfigを直接注入する。
     *
     * @param client
     *            IndexClient
     * @param config
     *            OpenSearchConfig
     */
    IndexManagementService(IndexClient client, OpenSearchConfig config) {
        this.client = client;
        this.openSearchClient = null;
        this.config = config;
    }

    /**
     * デフォルト設定でインデックスを作成する。
     *
     * @param indexName
     *            作成するインデックス名
     *
     * @return 作成が承認された場合 true
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public boolean createIndex(String indexName) throws IOException {
        return createIndex(indexName, null, false);
    }

    /**
     * マッピングを指定してインデックスを作成する。
     *
     * @param indexName
     *            作成するインデックス名
     * @param properties
     *            フィールドマッピング定義（null の場合はマッピングなし）
     *
     * @return 作成が承認された場合 true
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public boolean createIndex(String indexName, Map<String, Property> properties) throws IOException {
        return createIndex(indexName, properties, false);
    }

    /**
     * マッピングとk-NN設定を指定してインデックスを作成する。
     *
     * @param indexName
     *            作成するインデックス名
     * @param properties
     *            フィールドマッピング定義（null の場合はマッピングなし）
     * @param enableKnn
     *            k-NN機能を有効にする場合 true
     *
     * @return 作成が承認された場合 true
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public boolean createIndex(String indexName, Map<String, Property> properties, boolean enableKnn)
            throws IOException {
        logger.info("Creating index: {}, enableKnn={}", indexName, enableKnn);

        boolean acknowledged = client.createIndex(indexName, config.getNumberOfShards(), config.getNumberOfReplicas(),
                properties, enableKnn);

        logger.info("Index {} created: acknowledged={}", indexName, acknowledged);
        return acknowledged;
    }

    /**
     * インデックスを削除する。
     *
     * @param indexName
     *            削除するインデックス名
     *
     * @return 削除が承認された場合 true
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public boolean deleteIndex(String indexName) throws IOException {
        logger.info("Deleting index: {}", indexName);

        boolean acknowledged = client.deleteIndex(indexName);
        logger.info("Index {} deleted: acknowledged={}", indexName, acknowledged);
        return acknowledged;
    }

    /**
     * インデックスが存在するか確認する。
     *
     * @param indexName
     *            確認するインデックス名
     *
     * @return インデックスが存在する場合 true
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public boolean indexExists(String indexName) throws IOException {
        return client.indexExists(indexName);
    }

    /**
     * 既存インデックスのマッピングを更新する。
     * <p>
     * 注意: 既存フィールドの型変更はできません。新しいフィールドの追加のみ可能です。
     * </p>
     *
     * @param indexName
     *            更新するインデックス名
     * @param properties
     *            追加するフィールドマッピング定義
     *
     * @return 更新が承認された場合 true
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public boolean putMapping(String indexName, Map<String, Property> properties) throws IOException {
        logger.info("Updating mapping for index: {}", indexName);

        boolean acknowledged = client.putMapping(indexName, properties);

        logger.info("Mapping updated for index {}: acknowledged={}", indexName, acknowledged);
        return acknowledged;
    }

    /**
     * インデックステンプレートを作成する。
     * <p>
     * 指定したパターンに一致する新規インデックスに自動的に適用されるテンプレートを作成します。
     * </p>
     *
     * @param templateName
     *            テンプレート名
     * @param indexPattern
     *            適用対象のインデックスパターン（例: "logs-*"）
     * @param properties
     *            フィールドマッピング定義
     *
     * @return 作成が承認された場合 true
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public boolean putIndexTemplate(String templateName, String indexPattern, Map<String, Property> properties)
            throws IOException {
        logger.info("Creating index template: {}, pattern: {}", templateName, indexPattern);

        boolean acknowledged = client.putIndexTemplate(templateName, indexPattern, config.getNumberOfShards(),
                config.getNumberOfReplicas(), properties);

        logger.info("Index template {} created: acknowledged={}", templateName, acknowledged);
        return acknowledged;
    }

    /**
     * インデックステンプレートを削除する。
     *
     * @param templateName
     *            削除するテンプレート名
     *
     * @return 削除が承認された場合 true
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public boolean deleteIndexTemplate(String templateName) throws IOException {
        logger.info("Deleting index template: {}", templateName);

        boolean acknowledged = client.deleteIndexTemplate(templateName);
        logger.info("Index template {} deleted: acknowledged={}", templateName, acknowledged);
        return acknowledged;
    }

    /**
     * インデックスの設定を取得する。
     *
     * @param indexName
     *            設定を取得するインデックス名
     *
     * @return インデックス設定のレスポンス
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public GetIndicesSettingsResponse getIndexSettings(String indexName) throws IOException {
        return client.getIndexSettings(indexName);
    }

    /**
     * インデックスのマッピングを取得する。
     *
     * @param indexName
     *            マッピングを取得するインデックス名
     *
     * @return マッピング情報のレスポンス
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public GetMappingResponse getIndexMapping(String indexName) throws IOException {
        return client.getIndexMapping(indexName);
    }

    /**
     * インデックスをリフレッシュする。
     * <p>
     * インデックスされたドキュメントを検索可能な状態にします。 通常、OpenSearchは自動的にリフレッシュを行いますが、 即座に検索可能にしたい場合に使用します。
     * </p>
     *
     * @param indexName
     *            リフレッシュするインデックス名
     *
     * @return 常にtrue
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public boolean refreshIndex(String indexName) throws IOException {
        client.refreshIndex(indexName);
        return true;
    }

    /**
     * 複数のベクトルを一括登録する。
     * <p>
     * Bulk APIを使用して効率的に複数のベクトルを登録します。
     * </p>
     *
     * @param indexName
     *            登録先のインデックス名
     * @param vectorField
     *            ベクトルを格納するフィールド名
     * @param documents
     *            登録するベクトルドキュメントのリスト
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public void bulkIndexVectors(String indexName, String vectorField, List<VectorDocument> documents)
            throws IOException {
        logger.info("Bulk indexing {} vectors to index: {}", documents.size(), indexName);

        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

        for (VectorDocument doc : documents) {
            Map<String, Object> document = new HashMap<>();
            document.put(vectorField, toFloatList(doc.vector()));
            if (doc.metadata() != null) {
                document.putAll(doc.metadata());
            }
            bulkBuilder.operations(op -> op.index(i -> i.index(indexName).id(doc.id()).document(document)));
        }

        BulkResponse response = openSearchClient.bulk(bulkBuilder.build());
        logger.info("Bulk indexing completed. Errors: {}", response.errors());
    }

    /**
     * float配列をFloatのリストに変換する。
     *
     * @param array
     *            変換元の float 配列
     *
     * @return Float のリスト
     */
    private List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }

    /**
     * ベクトルドキュメントを表すレコード。
     * <p>
     * ベクトルとそのメタデータを一緒に保持します。
     * </p>
     *
     * @param id
     *            ドキュメント ID
     * @param vector
     *            ベクトルデータ
     * @param metadata
     *            関連するメタデータ（オプション）
     */
    @SuppressWarnings("java:S6218")
    public record VectorDocument(String id, float[] vector, Map<String, Object> metadata) {
    }
}