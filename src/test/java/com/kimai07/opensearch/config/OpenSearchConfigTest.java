package com.kimai07.opensearch.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.assertj.core.api.Assertions.assertThat;

class OpenSearchConfigTest {

    @Nested
    class DefaultConfig {
        @Test
        void testDefaultConfigHasExpectedValues() {
            OpenSearchConfig config = OpenSearchConfig.defaults();

            assertThat(config.getHost()).isEqualTo("localhost");
            assertThat(config.getPort()).isEqualTo(9200);
            assertThat(config.getScheme()).isEqualTo("http");
            assertThat(config.getConnectionTimeout()).isEqualTo(5000);
            assertThat(config.getSocketTimeout()).isEqualTo(60000);
            assertThat(config.getNumberOfShards()).isEqualTo(1);
            assertThat(config.getNumberOfReplicas()).isZero();
            assertThat(config.getKnnDimension()).isEqualTo(128);
            assertThat(config.getKnnSpaceType()).isEqualTo("l2");
        }
    }

    @Nested
    class BuilderConfig {
        @Test
        void testBuilderWithCustomValues() {
            OpenSearchConfig config = new OpenSearchConfig.Builder().host("custom-host").port(9201).scheme("https")
                    .connectionTimeout(10000).socketTimeout(120000).numberOfShards(3).numberOfReplicas(2)
                    .knnDimension(256).knnSpaceType("cosinesimil").build();

            assertThat(config.getHost()).isEqualTo("custom-host");
            assertThat(config.getPort()).isEqualTo(9201);
            assertThat(config.getScheme()).isEqualTo("https");
            assertThat(config.getConnectionTimeout()).isEqualTo(10000);
            assertThat(config.getSocketTimeout()).isEqualTo(120000);
            assertThat(config.getNumberOfShards()).isEqualTo(3);
            assertThat(config.getNumberOfReplicas()).isEqualTo(2);
            assertThat(config.getKnnDimension()).isEqualTo(256);
            assertThat(config.getKnnSpaceType()).isEqualTo("cosinesimil");
        }

        @Test
        void testBuilderWithZeroValues() {
            OpenSearchConfig config = new OpenSearchConfig.Builder().port(0).connectionTimeout(0).socketTimeout(0)
                    .numberOfShards(0).numberOfReplicas(0).knnDimension(0).build();

            assertThat(config.getPort()).isZero();
            assertThat(config.getConnectionTimeout()).isZero();
            assertThat(config.getSocketTimeout()).isZero();
            assertThat(config.getNumberOfShards()).isZero();
            assertThat(config.getNumberOfReplicas()).isZero();
            assertThat(config.getKnnDimension()).isZero();
        }

        @Test
        void testBuilderWithNegativeValues() {
            OpenSearchConfig config = new OpenSearchConfig.Builder().port(-1).connectionTimeout(-100)
                    .socketTimeout(-100).numberOfShards(-1).numberOfReplicas(-1).knnDimension(-1).build();

            assertThat(config.getPort()).isEqualTo(-1);
            assertThat(config.getConnectionTimeout()).isEqualTo(-100);
            assertThat(config.getNumberOfShards()).isEqualTo(-1);
        }

        @Test
        void testBuilderWithNullValues() {
            OpenSearchConfig config = new OpenSearchConfig.Builder().host(null).scheme(null).knnSpaceType(null).build();

            assertThat(config.getHost()).isNull();
            assertThat(config.getScheme()).isNull();
            assertThat(config.getKnnSpaceType()).isNull();
        }

        @Test
        void testBuilderWithEmptyStringValues() {
            OpenSearchConfig config = new OpenSearchConfig.Builder().host("").scheme("").knnSpaceType("").build();

            assertThat(config.getHost()).isEmpty();
            assertThat(config.getScheme()).isEmpty();
            assertThat(config.getKnnSpaceType()).isEmpty();
        }

        @Test
        void testBuilderChaining() {
            OpenSearchConfig.Builder builder = new OpenSearchConfig.Builder();

            // メソッドチェーンが正しく動作することを確認
            OpenSearchConfig config = builder.host("host1").host("host2") // 上書き
                    .port(9200).port(9201) // 上書き
                    .build();

            assertThat(config.getHost()).isEqualTo("host2");
            assertThat(config.getPort()).isEqualTo(9201);
        }
    }

    @Nested
    class ConnectionUrl {
        @Test
        void testGetConnectionUrlWithHttp() {
            OpenSearchConfig config = new OpenSearchConfig.Builder().scheme("http").host("localhost").port(9200)
                    .build();

            assertThat(config.getConnectionUrl()).isEqualTo("http://localhost:9200");
        }

        @Test
        void testGetConnectionUrlWithHttps() {
            OpenSearchConfig config = new OpenSearchConfig.Builder().scheme("https").host("my-host").port(443).build();

            assertThat(config.getConnectionUrl()).isEqualTo("https://my-host:443");
        }

        @Test
        void testGetConnectionUrlWithIpAddress() {
            OpenSearchConfig config = new OpenSearchConfig.Builder().scheme("http").host("192.168.1.100").port(9200)
                    .build();

            assertThat(config.getConnectionUrl()).isEqualTo("http://192.168.1.100:9200");
        }

        @Test
        void testGetConnectionUrlWithFqdn() {
            OpenSearchConfig config = new OpenSearchConfig.Builder().scheme("https").host("opensearch.example.com")
                    .port(9200).build();

            assertThat(config.getConnectionUrl()).isEqualTo("https://opensearch.example.com:9200");
        }
    }

    @Nested
    class FromProperties {
        @Test
        void testFromPropertiesLoadsDefaults() {
            OpenSearchConfig config = OpenSearchConfig.fromProperties();

            // application.properties のデフォルト値を確認
            assertThat(config.getHost()).isEqualTo("localhost");
            assertThat(config.getPort()).isEqualTo(9200);
            assertThat(config.getScheme()).isEqualTo("http");
        }
    }
}