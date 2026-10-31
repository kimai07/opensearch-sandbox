package com.kimai07.opensearch.api.search.regression;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * リグレッションテスト用のインデックスを作成し、データを投入する。
 */
final class RegressionData {

    private static final int NOT_FOUND = 404;
    private static final int OK = 200;

    private final HttpClient httpClient;
    private final String indexUrl;
    private final Path resources;

    RegressionData(HttpClient httpClient, String connectionUrl, String indexName, Path resources) {
        this.httpClient = httpClient;
        this.indexUrl = connectionUrl + "/" + indexName;
        this.resources = resources;
    }

    /**
     * インデックスを作り直してデータを投入する。投入データはすぐに検索できる状態になる。
     */
    void reset() throws IOException, InterruptedException {
        delete();
        send(json(HttpRequest.newBuilder(URI.create(indexUrl)), "PUT", "mapping.json"), "index creation");

        HttpRequest bulk = HttpRequest.newBuilder(URI.create(indexUrl + "/_bulk?refresh=true"))
                .header("Content-Type", "application/x-ndjson")
                .POST(HttpRequest.BodyPublishers.ofFile(resources.resolve("articles.ndjson"))).build();
        assertThat(send(bulk, "bulk indexing")).as("bulk indexing errors").contains("\"errors\":false");
    }

    /**
     * インデックスを削除する。存在しない場合は何もしない。
     */
    void delete() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(indexUrl)).DELETE().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("index deletion").isIn(OK, NOT_FOUND);
    }

    private HttpRequest json(HttpRequest.Builder builder, String method, String resource) throws IOException {
        return builder.header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(Files.readString(resources.resolve(resource))))
                .build();
    }

    private String send(HttpRequest request, String description) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(description + ": " + response.body()).isEqualTo(OK);
        return response.body();
    }
}
