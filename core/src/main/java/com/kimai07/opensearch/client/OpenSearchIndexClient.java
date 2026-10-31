package com.kimai07.opensearch.client;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.DeleteIndexResponse;
import org.opensearch.client.opensearch.indices.DeleteIndexTemplateResponse;
import org.opensearch.client.opensearch.indices.GetIndicesSettingsResponse;
import org.opensearch.client.opensearch.indices.GetMappingResponse;
import org.opensearch.client.opensearch.indices.PutIndexTemplateResponse;
import org.opensearch.client.opensearch.indices.PutMappingResponse;

import java.io.IOException;
import java.util.Map;

/**
 * IndexClientのOpenSearch実装。
 */
public class OpenSearchIndexClient implements IndexClient {

    private final OpenSearchClient client;

    public OpenSearchIndexClient(OpenSearchClient client) {
        this.client = client;
    }

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
    @Override
    public boolean createIndex(String indexName, int numberOfShards, int numberOfReplicas,
            Map<String, Property> properties, boolean enableKnn) throws IOException {
        CreateIndexRequest.Builder builder = new CreateIndexRequest.Builder().index(indexName).settings(s -> {
            s.numberOfShards(numberOfShards).numberOfReplicas(numberOfReplicas);
            if (enableKnn) {
                s.knn(true);
            }
            return s;
        });

        if (properties != null && !properties.isEmpty()) {
            builder.mappings(m -> m.properties(properties));
        }

        CreateIndexResponse response = client.indices().create(builder.build());
        return response.acknowledged();
    }

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
    @Override
    public boolean deleteIndex(String indexName) throws IOException {
        DeleteIndexResponse response = client.indices().delete(d -> d.index(indexName));
        return response.acknowledged();
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
    @Override
    public boolean indexExists(String indexName) throws IOException {
        return client.indices().exists(e -> e.index(indexName)).value();
    }

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
    @Override
    public boolean putMapping(String indexName, Map<String, Property> properties) throws IOException {
        PutMappingResponse response = client.indices().putMapping(p -> p.index(indexName).properties(properties));
        return response.acknowledged();
    }

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
    @Override
    public boolean putIndexTemplate(String templateName, String indexPattern, int numberOfShards, int numberOfReplicas,
            Map<String, Property> properties) throws IOException {
        PutIndexTemplateResponse response = client.indices()
                .putIndexTemplate(p -> p.name(templateName).indexPatterns(indexPattern)
                        .template(t -> t
                                .settings(s -> s.numberOfShards(numberOfShards).numberOfReplicas(numberOfReplicas))
                                .mappings(m -> m.properties(properties))));
        return response.acknowledged();
    }

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
    @Override
    public boolean deleteIndexTemplate(String templateName) throws IOException {
        DeleteIndexTemplateResponse response = client.indices().deleteIndexTemplate(d -> d.name(templateName));
        return response.acknowledged();
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
    @Override
    public GetIndicesSettingsResponse getIndexSettings(String indexName) throws IOException {
        return client.indices().getSettings(g -> g.index(indexName));
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
    @Override
    public GetMappingResponse getIndexMapping(String indexName) throws IOException {
        return client.indices().getMapping(g -> g.index(indexName));
    }

    /**
     * インデックスをリフレッシュする。
     *
     * @param indexName
     *            リフレッシュするインデックス名
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    @Override
    public void refreshIndex(String indexName) throws IOException {
        client.indices().refresh(r -> r.index(indexName));
    }
}
