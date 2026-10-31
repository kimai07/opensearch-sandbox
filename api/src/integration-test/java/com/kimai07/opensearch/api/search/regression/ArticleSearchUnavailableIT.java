package com.kimai07.opensearch.api.search.regression;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OpenSearchに接続できない場合の動作を確認する。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = { "opensearch.host=127.0.0.1",
        "opensearch.port=1" })
class ArticleSearchUnavailableIT {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("OpenSearchに接続できない場合、レスポンスが503で接続先の情報を含まない内容であること")
    void testSearchReturnsServiceUnavailable() throws IOException, InterruptedException {
        URI uri = URI.create("http://localhost:" + port + "/api/v1/articles/search?q=OpenSearch");
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.body()).doesNotContain("127.0.0.1").doesNotContain("Exception");
    }
}
