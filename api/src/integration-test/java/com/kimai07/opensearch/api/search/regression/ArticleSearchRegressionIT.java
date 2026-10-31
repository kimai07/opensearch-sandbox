package com.kimai07.opensearch.api.search.regression;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kimai07.opensearch.api.config.OpenSearchProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 検索APIのレスポンスを期待値と比較する。
 * <p>
 * 起動中のOpenSearchに軽量データを投入し、APIのステータスとボディを {@code regression/golden} の期待値と比較します。 {@code -Dregression.update=true}
 * を指定すると、比較せずに期待値を更新します。
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "opensearch.index=regression-articles")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArticleSearchRegressionIT {

    private static final Path RESOURCES = Path.of("src/integration-test/resources/regression");
    private static final Path GOLDEN = RESOURCES.resolve("golden");
    private static final boolean UPDATE = Boolean.getBoolean("regression.update");

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired
    private OpenSearchProperties properties;

    @LocalServerPort
    private int port;

    private RegressionData data;

    @BeforeAll
    void setUp() throws IOException, InterruptedException {
        if (UPDATE && System.getenv("CI") != null) {
            throw new IllegalStateException("Golden files must not be updated in CI");
        }
        data = new RegressionData(httpClient, properties.toConfig().getConnectionUrl(), properties.index(), RESOURCES);
        data.reset();
    }

    @AfterAll
    void tearDown() throws IOException, InterruptedException {
        data.delete();
    }

    List<RegressionCase> cases() throws IOException {
        return mapper.readValue(RESOURCES.resolve("cases.json").toFile(), new TypeReference<>() {
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    @DisplayName("ケースごとのステータスとボディが期待値と同値であること")
    void testResponseMatchesGolden(RegressionCase regressionCase) throws Exception {
        String actual = fetch(regressionCase);
        Path golden = GOLDEN.resolve(regressionCase.name() + ".json");

        if (UPDATE) {
            Files.createDirectories(GOLDEN);
            Files.writeString(golden, actual + System.lineSeparator());
            return;
        }
        JSONAssert.assertEquals(Files.readString(golden), actual, JSONCompareMode.STRICT);
    }

    private String fetch(RegressionCase regressionCase) throws IOException, InterruptedException {
        URI uri = URI.create("http://localhost:" + port + "/api/v1/articles/search?" + regressionCase.toQueryString());
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", response.statusCode());
        snapshot.put("body", mapper.readValue(response.body(), Object.class));
        return mapper.writeValueAsString(snapshot);
    }
}
