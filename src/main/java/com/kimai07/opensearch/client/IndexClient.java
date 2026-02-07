package com.kimai07.opensearch.client;

import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch.indices.GetIndicesSettingsResponse;
import org.opensearch.client.opensearch.indices.GetMappingResponse;

import java.io.IOException;
import java.util.Map;

/**
 * インデックス操作のインターフェース。 テスト時にモック可能にするために導入。
 */
public interface IndexClient {

    /**
     * インデックスを作成する。
     *
     * @param indexName
     *            作成するインデックス名
     * @param numberOfShards
     *            シャード数
     * @param numberOfReplicas
     *            レプリカ数
     * @param properties
     *            フィールドマッピング定義（nullの場合はマッピングなし）
     * @param enableKnn
     *            k-NN機能を有効にするかどうか
     *
     * @return 作成が成功した場合 true
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    boolean createIndex(String indexName, int numberOfShards, int numberOfReplicas, Map<String, Property> properties,
            boolean enableKnn) throws IOException;

    /**
     * インデックスを削除する。
     *
     * @param indexName
     *            削除するインデックス名
     *
     * @return 削除が成功した場合 true
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    boolean deleteIndex(String indexName) throws IOException;

    /**
     * インデックスが存在するか確認する。
     *
     * @param indexName
     *            確認するインデックス名
     *
     * @return インデックスが存在する場合 true
     */
    boolean indexExists(String indexName) throws IOException;

    /**
     * 既存インデックスのマッピングを更新する。
     *
     * @param indexName
     *            更新するインデックス名
     * @param properties
     *            追加するフィールドマッピング定義
     *
     * @return 更新が成功した場合 true
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    boolean putMapping(String indexName, Map<String, Property> properties) throws IOException;

    /**
     * インデックステンプレートを作成する。
     *
     * @param templateName
     *            テンプレート名
     * @param indexPattern
     *            適用対象のインデックスパターン
     * @param numberOfShards
     *            シャード数
     * @param numberOfReplicas
     *            レプリカ数
     * @param properties
     *            フィールドマッピング定義
     *
     * @return 作成が成功した場合 true
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    boolean putIndexTemplate(String templateName, String indexPattern, int numberOfShards, int numberOfReplicas,
            Map<String, Property> properties) throws IOException;

    /**
     * インデックステンプレートを削除する。
     *
     * @param templateName
     *            削除するテンプレート名
     *
     * @return 削除が成功した場合 true
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    boolean deleteIndexTemplate(String templateName) throws IOException;

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
    GetIndicesSettingsResponse getIndexSettings(String indexName) throws IOException;

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
    GetMappingResponse getIndexMapping(String indexName) throws IOException;

    /**
     * インデックスをリフレッシュする。
     *
     * @param indexName
     *            リフレッシュするインデックス名
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    void refreshIndex(String indexName) throws IOException;
}