package com.kimai07.opensearch.examples;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * 検索結果の出力を担当するユーティリティクラス。
 */
public final class SearchResultPrinter {

    private static final Logger logger = LoggerFactory.getLogger(SearchResultPrinter.class);
    private static final String FIELD_TITLE = "title";

    private SearchResultPrinter() {
    }

    /**
     * 検索結果を出力する。
     *
     * @param response
     *            検索レスポンス
     */
    public static void print(SearchResponse<ObjectNode> response) {
        logger.info("Found {} hits:", Objects.requireNonNull(response.hits().total()).value());
        for (var hit : response.hits().hits()) {
            logger.info("  - [{}] score={}: {}", hit.id(), hit.score(),
                    hit.source() != null ? hit.source().get(FIELD_TITLE) : "N/A");
        }
        logger.info("");
    }

    /**
     * ハイライト付きの検索結果を出力する。
     *
     * @param response
     *            検索レスポンス
     */
    public static void printWithHighlight(SearchResponse<ObjectNode> response) {
        logger.info("Found {} hits with highlights:", Objects.requireNonNull(response.hits().total()).value());
        for (var hit : response.hits().hits()) {
            logger.info("  - [{}] {}", hit.id(), hit.source() != null ? hit.source().get(FIELD_TITLE) : "N/A");
            if (!hit.highlight().isEmpty()) {
                for (var entry : hit.highlight().entrySet()) {
                    logger.info("    Highlight ({}): {}", entry.getKey(), entry.getValue());
                }
            }
        }
        logger.info("");
    }

    /**
     * k-NN検索結果を出力する。
     *
     * @param response
     *            検索レスポンス
     */
    public static void printKnnResults(SearchResponse<ObjectNode> response) {
        logger.info("Found {} nearest neighbors:", Objects.requireNonNull(response.hits().total()).value());
        for (var hit : response.hits().hits()) {
            String title = hit.source() != null && hit.source().has(FIELD_TITLE)
                    ? hit.source().get(FIELD_TITLE).asText() : "N/A";
            String category = hit.source() != null && hit.source().has("category")
                    ? hit.source().get("category").asText() : "N/A";
            logger.info("  - [{}] score={}: {} (category: {})", hit.id(), hit.score(), title, category);
        }
        logger.info("");
    }
}