package com.kimai07.opensearch.search;

import com.kimai07.opensearch.client.OpenSearchClientFactory;
import com.kimai07.opensearch.client.OpenSearchSearchClient;
import com.kimai07.opensearch.client.SearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Highlight;
import org.opensearch.client.opensearch.core.search.HighlightField;
import org.opensearch.client.opensearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * OpenSearchの全文検索サービス。
 * <p>
 * Match Query、Bool Query、Fuzzy Query、ハイライトなど、 様々な全文検索機能を提供します。
 * </p>
 *
 * @see OpenSearchClientFactory
 */
public class FullTextSearchService {

    private static final Logger logger = LoggerFactory.getLogger(FullTextSearchService.class);

    private final SearchClient client;

    /**
     * FullTextSearchServiceを構築する。
     *
     * @param factory
     *            OpenSearch クライアントファクトリ
     */
    public FullTextSearchService(OpenSearchClientFactory factory) {
        this.client = new OpenSearchSearchClient(factory.getClient());
    }

    /**
     * テスト用コンストラクタ。SearchClientを直接注入する。
     *
     * @param client
     *            SearchClient
     */
    FullTextSearchService(SearchClient client) {
        this.client = client;
    }

    /**
     * Match Queryで検索を実行する。
     * <p>
     * 指定したフィールドに対して全文検索を行います。 クエリ文字列はアナライザーによってトークン化されます。
     * </p>
     *
     * @param <T>
     *            検索結果のドキュメント型
     * @param indexName
     *            検索対象のインデックス名
     * @param field
     *            検索対象のフィールド名
     * @param query
     *            検索クエリ文字列
     * @param clazz
     *            結果をマッピングするクラス
     *
     * @return 検索結果の SearchResponse
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public <T> SearchResponse<T> matchQuery(String indexName, String field, String query, Class<T> clazz)
            throws IOException {
        logger.info("Executing match query on index: {}, field: {}, query: {}", indexName, field, query);

        SearchRequest request = new SearchRequest.Builder().index(indexName)
                .query(q -> q.match(m -> m.field(field).query(FieldValue.of(query)))).build();

        SearchResponse<T> response = client.search(request, clazz);
        logger.info("Match query returned {} hits", Objects.requireNonNull(response.hits().total()).value());
        return response;
    }

    /**
     * Multi Match Queryで複数フィールドを検索する。
     * <p>
     * 複数のフィールドに対して同時に検索を行います。 いずれかのフィールドがマッチすればヒットします。
     * </p>
     *
     * @param <T>
     *            検索結果のドキュメント型
     * @param indexName
     *            検索対象のインデックス名
     * @param fields
     *            検索対象のフィールド名リスト
     * @param query
     *            検索クエリ文字列
     * @param clazz
     *            結果をマッピングするクラス
     *
     * @return 検索結果の SearchResponse
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public <T> SearchResponse<T> multiMatchQuery(String indexName, List<String> fields, String query, Class<T> clazz)
            throws IOException {
        logger.info("Executing multi-match query on index: {}, fields: {}, query: {}", indexName, fields, query);

        SearchRequest request = new SearchRequest.Builder().index(indexName)
                .query(q -> q.multiMatch(m -> m.fields(fields).query(query))).build();

        SearchResponse<T> response = client.search(request, clazz);
        logger.info("Multi-match query returned {} hits", Objects.requireNonNull(response.hits().total()).value());
        return response;
    }

    /**
     * Bool Queryで複合条件検索を実行する。
     * <p>
     * must（必須）、should（あれば良い）、mustNot（除外）の条件を組み合わせて検索します。
     * </p>
     *
     * @param <T>
     *            検索結果のドキュメント型
     * @param indexName
     *            検索対象のインデックス名
     * @param must
     *            必須条件のクエリリスト（null または空の場合は無視）
     * @param should
     *            オプション条件のクエリリスト（null または空の場合は無視）
     * @param mustNot
     *            除外条件のクエリリスト（null または空の場合は無視）
     * @param clazz
     *            結果をマッピングするクラス
     *
     * @return 検索結果の SearchResponse
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public <T> SearchResponse<T> boolQuery(String indexName, List<Query> must, List<Query> should, List<Query> mustNot,
            Class<T> clazz) throws IOException {
        logger.info("Executing bool query on index: {}", indexName);

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        if (must != null && !must.isEmpty()) {
            boolBuilder.must(must);
        }
        if (should != null && !should.isEmpty()) {
            boolBuilder.should(should);
        }
        if (mustNot != null && !mustNot.isEmpty()) {
            boolBuilder.mustNot(mustNot);
        }

        SearchRequest request = new SearchRequest.Builder().index(indexName).query(q -> q.bool(boolBuilder.build()))
                .build();

        SearchResponse<T> response = client.search(request, clazz);
        logger.info("Bool query returned {} hits", Objects.requireNonNull(response.hits().total()).value());
        return response;
    }

    /**
     * Fuzzy Queryであいまい検索を実行する。
     * <p>
     * 編集距離（レーベンシュタイン距離）に基づいて、 類似した文字列を検索します。タイプミスの許容に有効です。
     * </p>
     *
     * @param <T>
     *            検索結果のドキュメント型
     * @param indexName
     *            検索対象のインデックス名
     * @param field
     *            検索対象のフィールド名
     * @param value
     *            検索する値
     * @param fuzziness
     *            あいまい度（"AUTO", "0", "1", "2"など）
     * @param clazz
     *            結果をマッピングするクラス
     *
     * @return 検索結果の SearchResponse
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public <T> SearchResponse<T> fuzzyQuery(String indexName, String field, String value, String fuzziness,
            Class<T> clazz) throws IOException {
        logger.info("Executing fuzzy query on index: {}, field: {}, value: {}, fuzziness: {}", indexName, field, value,
                fuzziness);

        SearchRequest request = new SearchRequest.Builder().index(indexName)
                .query(q -> q.fuzzy(f -> f.field(field).value(FieldValue.of(value)).fuzziness(fuzziness))).build();

        SearchResponse<T> response = client.search(request, clazz);
        logger.info("Fuzzy query returned {} hits", Objects.requireNonNull(response.hits().total()).value());
        return response;
    }

    /**
     * ハイライト付きで検索を実行する。
     * <p>
     * マッチした部分をHTMLタグ（デフォルトは&lt;em&gt;）で囲んで返します。
     * </p>
     *
     * @param <T>
     *            検索結果のドキュメント型
     * @param indexName
     *            検索対象のインデックス名
     * @param field
     *            検索およびハイライト対象のフィールド名
     * @param query
     *            検索クエリ文字列
     * @param clazz
     *            結果をマッピングするクラス
     *
     * @return 検索結果のSearchResponse（ハイライト情報を含む）
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public <T> SearchResponse<T> searchWithHighlight(String indexName, String field, String query, Class<T> clazz)
            throws IOException {
        logger.info("Executing search with highlight on index: {}, field: {}, query: {}", indexName, field, query);

        Highlight highlight = new Highlight.Builder()
                .fields(field, new HighlightField.Builder().preTags("<em>").postTags("</em>").build()).build();

        SearchRequest request = new SearchRequest.Builder().index(indexName)
                .query(q -> q.match(m -> m.field(field).query(FieldValue.of(query)))).highlight(highlight).build();

        SearchResponse<T> response = client.search(request, clazz);
        logger.info("Search with highlight returned {} hits", Objects.requireNonNull(response.hits().total()).value());
        return response;
    }

    /**
     * Phrase Match Queryでフレーズ検索を実行する。
     * <p>
     * 指定した語順通りに連続するフレーズを検索します。
     * </p>
     *
     * @param <T>
     *            検索結果のドキュメント型
     * @param indexName
     *            検索対象のインデックス名
     * @param field
     *            検索対象のフィールド名
     * @param phrase
     *            検索するフレーズ
     * @param clazz
     *            結果をマッピングするクラス
     *
     * @return 検索結果の SearchResponse
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public <T> SearchResponse<T> phraseMatchQuery(String indexName, String field, String phrase, Class<T> clazz)
            throws IOException {
        logger.info("Executing phrase match query on index: {}, field: {}, phrase: {}", indexName, field, phrase);

        SearchRequest request = new SearchRequest.Builder().index(indexName)
                .query(q -> q.matchPhrase(m -> m.field(field).query(phrase))).build();

        SearchResponse<T> response = client.search(request, clazz);
        logger.info("Phrase match query returned {} hits", Objects.requireNonNull(response.hits().total()).value());
        return response;
    }

    /**
     * Wildcard Queryでワイルドカード検索を実行する。
     * <p>
     * "*"（0文字以上）や"?"（1文字）のワイルドカードパターンで検索します。 注意: パフォーマンスに影響する可能性があるため、先頭のワイルドカードは避けてください。
     * </p>
     *
     * @param <T>
     *            検索結果のドキュメント型
     * @param indexName
     *            検索対象のインデックス名
     * @param field
     *            検索対象のフィールド名
     * @param pattern
     *            ワイルドカードパターン（例: "test*", "te?t"）
     * @param clazz
     *            結果をマッピングするクラス
     *
     * @return 検索結果の SearchResponse
     *
     * @throws IOException
     *             OpenSearch との通信エラーが発生した場合
     */
    public <T> SearchResponse<T> wildcardQuery(String indexName, String field, String pattern, Class<T> clazz)
            throws IOException {
        logger.info("Executing wildcard query on index: {}, field: {}, pattern: {}", indexName, field, pattern);

        SearchRequest request = new SearchRequest.Builder().index(indexName)
                .query(q -> q.wildcard(w -> w.field(field).value(pattern))).build();

        SearchResponse<T> response = client.search(request, clazz);
        logger.info("Wildcard query returned {} hits", Objects.requireNonNull(response.hits().total()).value());
        return response;
    }

    /**
     * 検索結果からドキュメントのリストを抽出する。
     *
     * @param <T>
     *            ドキュメントの型
     * @param response
     *            検索レスポンス
     *
     * @return ドキュメントのリスト
     */
    public <T> List<T> extractDocuments(SearchResponse<T> response) {
        return response.hits().hits().stream().map(Hit::source).toList();
    }

    /**
     * 検索ヒットからハイライト情報を抽出する。
     *
     * @param <T>
     *            ドキュメントの型
     * @param hit
     *            検索ヒット
     *
     * @return フィールド名をキー、ハイライトされたフラグメントのリストを値とするMap
     */
    public <T> Map<String, List<String>> extractHighlights(Hit<T> hit) {
        return hit.highlight();
    }
}