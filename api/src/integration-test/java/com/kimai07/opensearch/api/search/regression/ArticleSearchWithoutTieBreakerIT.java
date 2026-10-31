package com.kimai07.opensearch.api.search.regression;

import com.kimai07.opensearch.api.config.OpenSearchProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 同点用フィールドがないインデックスでも検索できることを確認する。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "opensearch.index=regression-no-tie-breaker")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArticleSearchWithoutTieBreakerIT {

    private static final Path RESOURCES = Path.of("src/integration-test/resources/regression");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired
    private OpenSearchProperties properties;

    @LocalServerPort
    private int port;

    private RegressionData data;

    @BeforeAll
    void setUp() throws IOException, InterruptedException {
        data = new RegressionData(httpClient, properties.toConfig().getConnectionUrl(), properties.index(), RESOURCES);
        data.reset("no-tie-breaker-mapping.json", "no-tie-breaker-articles.ndjson");
    }

    @AfterAll
    void tearDown() throws IOException, InterruptedException {
        data.delete();
    }

    @Test
    @DisplayName("article_idがないインデックスの場合、レスポンスが200で検索結果を含む内容であること")
    void testSearchSucceedsWithoutTieBreakerField() throws IOException, InterruptedException {
        URI uri = URI.create("http://localhost:" + port + "/api/v1/articles/search?q=OpenSearch");
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Introduction to");
    }
}
