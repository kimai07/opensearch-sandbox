package com.kimai07.opensearch.api.config;

import com.kimai07.opensearch.config.OpenSearchConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenSearchPropertiesTest {

    @Test
    @DisplayName("接続設定の各項目がOpenSearchConfigの値であること")
    void testToConfigConvertsConnectionSettings() {
        OpenSearchProperties properties = new OpenSearchProperties("search.example.com", 443, "https", 1500, 2500,
                "articles");

        OpenSearchConfig config = properties.toConfig();

        assertThat(config.getHost()).isEqualTo("search.example.com");
        assertThat(config.getPort()).isEqualTo(443);
        assertThat(config.getScheme()).isEqualTo("https");
        assertThat(config.getConnectionTimeout()).isEqualTo(1500);
        assertThat(config.getSocketTimeout()).isEqualTo(2500);
    }
}
