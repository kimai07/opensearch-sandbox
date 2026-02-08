package com.kimai07.opensearch.search;

import com.kimai07.opensearch.client.OpenSearchClientFactory;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * OpenSearchのベクトル検索（k-NN）サービス。
 * <p>
 * ベクトルデータのインデックス登録およびk-NN（k最近傍）検索機能を提供します。 類似検索、レコメンデーション、セマンティック検索などに使用できます。
 * </p>
 *
 * @see OpenSearchClientFactory
 */
public class VectorSearchService {

    private static final Logger logger = LoggerFactory.getLogger(VectorSearchService.class);

    private final OpenSearchClient client;

    /**
     * VectorSearchServiceを構築する。
     *
     * @param factory
     *            OpenSearch クライアントファクトリ
     */
    public VectorSearchService(OpenSearchClientFactory factory) {
        this.client = factory.getClient();
    }

    /**
     * k-NN検索を実行する。
     * <p>
     * 指定したクエリベクトルに最も近いk個のベクトルを検索します。
     * </p>
     *
     * @param <T>
     *            検索結果のドキュメント型
     * @param indexName
     *            検索対象のインデックス名
     * @param vectorField
     *            ベクトルが格納されているフィールド名
     * @param queryVector
     *            検索クエリベクトル
     * @param k
     *            返す最近傍の数
     * @param clazz
     *            結果をマッピングするクラス
     *
     * @return 検索結果の SearchResponse
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public <T> SearchResponse<T> knnSearch(String indexName, String vectorField, float[] queryVector, int k,
            Class<T> clazz) throws IOException {
        logger.info("Executing k-NN search on index: {}, field: {}, k: {}", indexName, vectorField, k);

        final List<Float> vector = toFloatList(queryVector);

        SearchRequest request = new SearchRequest.Builder().index(indexName)
                .query(q -> q.knn(knn -> knn.field(vectorField).vector(vector).k(k))).size(k).build();

        SearchResponse<T> response = client.search(request, clazz);
        logger.info("k-NN search returned {} hits", Objects.requireNonNull(response.hits().total()).value());
        return response;
    }

    /**
     * フィルタ付きk-NN検索を実行する。
     * <p>
     * 指定したフィルタ条件を満たすドキュメントの中から、 クエリベクトルに最も近いk個を検索します。
     * </p>
     *
     * @param <T>
     *            検索結果のドキュメント型
     * @param indexName
     *            検索対象のインデックス名
     * @param vectorField
     *            ベクトルが格納されているフィールド名
     * @param queryVector
     *            検索クエリベクトル
     * @param k
     *            返す最近傍の数
     * @param filter
     *            適用するフィルタクエリ
     * @param clazz
     *            結果をマッピングするクラス
     *
     * @return 検索結果の SearchResponse
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public <T> SearchResponse<T> knnSearchWithFilter(String indexName, String vectorField, float[] queryVector, int k,
            Query filter, Class<T> clazz) throws IOException {
        logger.info("Executing k-NN search with filter on index: {}, field: {}, k: {}", indexName, vectorField, k);

        final List<Float> vector = toFloatList(queryVector);

        SearchRequest request = new SearchRequest.Builder().index(indexName)
                .query(q -> q.knn(knn -> knn.field(vectorField).vector(vector).k(k).filter(filter))).size(k).build();

        SearchResponse<T> response = client.search(request, clazz);
        logger.info("k-NN search with filter returned {} hits",
                Objects.requireNonNull(response.hits().total()).value());
        return response;
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

}