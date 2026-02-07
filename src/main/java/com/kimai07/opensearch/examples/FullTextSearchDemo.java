package com.kimai07.opensearch.examples;

import com.kimai07.opensearch.index.IndexManagementService;
import com.kimai07.opensearch.search.FullTextSearchService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 全文検索機能のデモンストレーション。
 */
public class FullTextSearchDemo {

    private static final Logger logger = LoggerFactory.getLogger(FullTextSearchDemo.class);
    private static final String INDEX_NAME = "demo-articles";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_CATEGORY = "category";
    private static final String SEARCH_TERM = "OpenSearch";

    private final OpenSearchClient client;
    private final IndexManagementService indexService;
    private final FullTextSearchService searchService;

    public FullTextSearchDemo(OpenSearchClient client, IndexManagementService indexService,
            FullTextSearchService searchService) {
        this.client = client;
        this.indexService = indexService;
        this.searchService = searchService;
    }

    /**
     * デモを実行する。
     */
    public void run() throws IOException, InterruptedException {
        logger.info("\n--- Full Text Search Demo ---\n");

        setupIndex();
        indexSampleData();
        waitForIndexRefresh();

        runMatchQueryDemo();
        runBoolQueryDemo();
        runFuzzyQueryDemo();
        runHighlightDemo();
    }

    /**
     * インデックス名を取得する（クリーンアップ用）。
     */
    public String getIndexName() {
        return INDEX_NAME;
    }

    private void setupIndex() throws IOException {
        if (indexService.indexExists(INDEX_NAME)) {
            indexService.deleteIndex(INDEX_NAME);
        }

        Map<String, Property> properties = Map.of(FIELD_TITLE, Property.of(p -> p.text(t -> t)), FIELD_CONTENT,
                Property.of(p -> p.text(t -> t)), FIELD_CATEGORY, Property.of(p -> p.keyword(k -> k)));
        indexService.createIndex(INDEX_NAME, properties);
    }

    private void indexSampleData() throws IOException {
        logger.info("Indexing sample articles...");

        List<Map<String, Object>> articles = List.of(Map.of(FIELD_TITLE, "Introduction to OpenSearch", FIELD_CONTENT,
                "OpenSearch is a powerful open-source search and analytics engine.", FIELD_CATEGORY, "technology"),
                Map.of(FIELD_TITLE, "Full-Text Search Guide", FIELD_CONTENT,
                        "Learn how to implement full-text search with OpenSearch queries.", FIELD_CATEGORY, "tutorial"),
                Map.of(FIELD_TITLE, "Vector Search Basics", FIELD_CONTENT,
                        "OpenSearch supports k-NN vector search for similarity matching.", FIELD_CATEGORY,
                        "technology"),
                Map.of(FIELD_TITLE, "Data Analytics with OpenSearch", FIELD_CONTENT,
                        "Use OpenSearch for powerful data analytics and visualization.", FIELD_CATEGORY, "analytics"));

        for (int i = 0; i < articles.size(); i++) {
            final int docId = i + 1;
            final Map<String, Object> article = articles.get(i);
            client.index(idx -> idx.index(INDEX_NAME).id(String.valueOf(docId)).document(article));
        }

        logger.info("Indexed {} articles", articles.size());
    }

    private void waitForIndexRefresh() throws IOException, InterruptedException {
        indexService.refreshIndex(INDEX_NAME);
        Thread.sleep(1000);
    }

    private void runMatchQueryDemo() throws IOException {
        logger.info("1. Match Query: searching for 'OpenSearch'");
        SearchResponse<ObjectNode> result = searchService.matchQuery(INDEX_NAME, FIELD_CONTENT, SEARCH_TERM,
                ObjectNode.class);
        SearchResultPrinter.print(result);
    }

    private void runBoolQueryDemo() throws IOException {
        logger.info("2. Bool Query: must contain 'search', should contain 'powerful'");
        List<Query> must = List.of(Query.of(q -> q.match(m -> m.field(FIELD_CONTENT).query(FieldValue.of("search")))));
        List<Query> should = List
                .of(Query.of(q -> q.match(m -> m.field(FIELD_CONTENT).query(FieldValue.of("powerful")))));
        SearchResponse<ObjectNode> result = searchService.boolQuery(INDEX_NAME, must, should, null, ObjectNode.class);
        SearchResultPrinter.print(result);
    }

    private void runFuzzyQueryDemo() throws IOException {
        logger.info("3. Fuzzy Query: searching for 'OpenSearch' (typo)");
        SearchResponse<ObjectNode> result = searchService.fuzzyQuery(INDEX_NAME, FIELD_CONTENT, SEARCH_TERM, "2",
                ObjectNode.class);
        SearchResultPrinter.print(result);
    }

    private void runHighlightDemo() throws IOException {
        logger.info("4. Search with Highlight: 'OpenSearch'");
        SearchResponse<ObjectNode> result = searchService.searchWithHighlight(INDEX_NAME, FIELD_CONTENT, SEARCH_TERM,
                ObjectNode.class);
        SearchResultPrinter.printWithHighlight(result);
    }
}