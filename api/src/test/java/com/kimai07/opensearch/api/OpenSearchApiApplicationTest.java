package com.kimai07.opensearch.api;

import com.kimai07.opensearch.client.OpenSearchClientFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = { "opensearch.host=search.example.com", "opensearch.port=443", "opensearch.scheme=https" })
class OpenSearchApiApplicationTest {

    @Autowired
    private OpenSearchClientFactory clientFactory;

    @Test
    @DisplayName("接続先を上書きした場合、クライアントの接続先が上書きした値であること")
    void testConnectionSettingsCanBeOverridden() {
        assertThat(clientFactory.getConfig().getConnectionUrl()).isEqualTo("https://search.example.com:443");
    }
}
