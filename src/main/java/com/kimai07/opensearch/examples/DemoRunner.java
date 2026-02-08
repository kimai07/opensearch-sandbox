package com.kimai07.opensearch.examples;

import com.kimai07.opensearch.client.OpenSearchClientFactory;
import com.kimai07.opensearch.index.IndexManagementService;
import com.kimai07.opensearch.search.FullTextSearchService;
import com.kimai07.opensearch.search.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * OpenSearch機能のデモンストレーションランナー。
 * <p>
 * 各デモクラスを順に実行し、最後にクリーンアップを行います。
 * </p>
 */
public class DemoRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoRunner.class);

    private final OpenSearchClientFactory clientFactory;
    private final FullTextSearchDemo fullTextSearchDemo;
    private final VectorSearchDemo vectorSearchDemo;
    private final IndexManagementService indexService;

    /**
     * DemoRunnerを構築する。
     */
    public DemoRunner() {
        this.clientFactory = OpenSearchClientFactory.create();
        this.indexService = new IndexManagementService(clientFactory);

        this.fullTextSearchDemo = new FullTextSearchDemo(clientFactory.getClient(), indexService,
                new FullTextSearchService(clientFactory));

        this.vectorSearchDemo = new VectorSearchDemo(indexService, new VectorSearchService(clientFactory));
    }

    /**
     * アプリケーションのエントリーポイント。
     */
    @SuppressWarnings("java:S2142")
    public static void main(String[] args) {
        logger.info("Args: {}", (Object) args);
        DemoRunner runner = new DemoRunner();
        try {
            runner.run();
        } catch (Exception e) {
            logger.error("Demo failed", e);
            System.exit(1);
        }
    }

    /**
     * デモを実行する。
     */
    public void run() throws IOException, InterruptedException {
        logger.info("=== OpenSearch Sandbox Demo ===\n");

        if (!testConnection()) {
            return;
        }

        runDemos();
        cleanup();

        logger.info("\n=== Demo completed successfully! ===");
        clientFactory.close();
    }

    private boolean testConnection() {
        if (!clientFactory.testConnection()) {
            logger.error("Failed to connect to OpenSearch. Make sure OpenSearch is running.");
            logger.error("Run: docker compose up -d");
            return false;
        }
        return true;
    }

    private void runDemos() throws IOException, InterruptedException {
        fullTextSearchDemo.run();
        vectorSearchDemo.run();
    }

    private void cleanup() throws IOException {
        logger.info("\n--- Cleanup Demo Indices ---\n");

        List<String> indices = List.of(fullTextSearchDemo.getIndexName(), vectorSearchDemo.getIndexName());

        for (String indexName : indices) {
            if (indexService.indexExists(indexName)) {
                indexService.deleteIndex(indexName);
                logger.info("Deleted index: {}", indexName);
            }
        }
    }
}