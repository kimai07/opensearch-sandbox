package com.kimai07.opensearch.examples;

import com.kimai07.opensearch.index.IndexManagementService;
import com.kimai07.opensearch.search.VectorSearchService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * ベクトル検索（k-NN）機能のデモンストレーション。
 */
public class VectorSearchDemo {

    private static final Logger logger = LoggerFactory.getLogger(VectorSearchDemo.class);
    private static final String INDEX_NAME = "demo-vectors";
    private static final String VECTOR_FIELD = "embedding";
    private static final int DIMENSION = 128;
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_CATEGORY = "category";
    private static final String CATEGORY_SCIENCE = "science";

    private final IndexManagementService indexService;
    private final VectorSearchService searchService;
    private final Random random = new Random();

    public VectorSearchDemo(IndexManagementService indexService, VectorSearchService searchService) {
        this.indexService = indexService;
        this.searchService = searchService;
    }

    /**
     * デモを実行する。
     */
    public void run() throws IOException, InterruptedException {
        logger.info("\n--- Vector Search (k-NN) Demo ---\n");

        setupIndex();
        indexSampleData();
        waitForIndexRefresh();

        runKnnSearchDemo();
        runFilteredKnnSearchDemo();
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

        Map<String, Property> properties = Map.of(FIELD_TITLE, Property.of(p -> p.text(t -> t)), FIELD_CATEGORY,
                Property.of(p -> p.keyword(k -> k)), VECTOR_FIELD,
                Property.of(p -> p.knnVector(kv -> kv.dimension(DIMENSION))));
        indexService.createIndex(INDEX_NAME, properties, true);
    }

    private void indexSampleData() throws IOException {
        logger.info("Indexing sample vectors...");

        List<IndexManagementService.VectorDocument> documents = List.of(
                new IndexManagementService.VectorDocument("vec1", generateRandomVector(),
                        Map.of(FIELD_TITLE, "Document A", FIELD_CATEGORY, CATEGORY_SCIENCE)),
                new IndexManagementService.VectorDocument("vec2", generateRandomVector(),
                        Map.of(FIELD_TITLE, "Document B", FIELD_CATEGORY, "technology")),
                new IndexManagementService.VectorDocument("vec3", generateRandomVector(),
                        Map.of(FIELD_TITLE, "Document C", FIELD_CATEGORY, CATEGORY_SCIENCE)),
                new IndexManagementService.VectorDocument("vec4", generateRandomVector(),
                        Map.of(FIELD_TITLE, "Document D", FIELD_CATEGORY, "arts")),
                new IndexManagementService.VectorDocument("vec5", generateRandomVector(),
                        Map.of(FIELD_TITLE, "Document E", FIELD_CATEGORY, CATEGORY_SCIENCE)));

        indexService.bulkIndexVectors(INDEX_NAME, VECTOR_FIELD, documents);
        logger.info("Indexed {} vectors", documents.size());
    }

    private void waitForIndexRefresh() throws IOException, InterruptedException {
        indexService.refreshIndex(INDEX_NAME);
        Thread.sleep(1000);
    }

    private void runKnnSearchDemo() throws IOException {
        logger.info("1. k-NN Search: finding 3 nearest neighbors");
        float[] queryVector = generateRandomVector();
        SearchResponse<ObjectNode> result = searchService.knnSearch(INDEX_NAME, VECTOR_FIELD, queryVector, 3,
                ObjectNode.class);
        SearchResultPrinter.printKnnResults(result);
    }

    private void runFilteredKnnSearchDemo() throws IOException {
        logger.info("2. k-NN Search with Filter: category='science'");
        float[] queryVector = generateRandomVector();
        Query filter = Query.of(q -> q.term(t -> t.field(FIELD_CATEGORY).value(FieldValue.of(CATEGORY_SCIENCE))));
        SearchResponse<ObjectNode> result = searchService.knnSearchWithFilter(INDEX_NAME, VECTOR_FIELD, queryVector, 3,
                filter, ObjectNode.class);
        SearchResultPrinter.printKnnResults(result);
    }

    private float[] generateRandomVector() {
        float[] vector = new float[DIMENSION];
        for (int i = 0; i < DIMENSION; i++) {
            vector[i] = random.nextFloat();
        }
        return vector;
    }
}