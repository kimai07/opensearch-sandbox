package com.kimai07.opensearch.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class OpenSearchConfigTest {

    @Nested
    class DefaultConfig {
        @Test
        @DisplayName("デフォルト設定の各項目が既定値であること")
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
        @DisplayName("カスタム値を指定した場合、各項目が指定値であること")
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
        @DisplayName("0を指定した場合、各項目が0であること")
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
        @DisplayName("負の値を指定した場合、各項目が指定した負の値であること")
        void testBuilderWithNegativeValues() {
            OpenSearchConfig config = new OpenSearchConfig.Builder().port(-1).connectionTimeout(-100)
                    .socketTimeout(-100).numberOfShards(-1).numberOfReplicas(-1).knnDimension(-1).build();

            assertThat(config.getPort()).isEqualTo(-1);
            assertThat(config.getConnectionTimeout()).isEqualTo(-100);
            assertThat(config.getNumberOfShards()).isEqualTo(-1);
        }

        @Test
        @DisplayName("nullを指定した場合、各項目がnullであること")
        void testBuilderWithNullValues() {
            OpenSearchConfig config = new OpenSearchConfig.Builder().host(null).scheme(null).knnSpaceType(null).build();

            assertThat(config.getHost()).isNull();
            assertThat(config.getScheme()).isNull();
            assertThat(config.getKnnSpaceType()).isNull();
        }

        @Test
        @DisplayName("空文字を指定した場合、各項目が空文字であること")
        void testBuilderWithEmptyStringValues() {
            OpenSearchConfig config = new OpenSearchConfig.Builder().host("").scheme("").knnSpaceType("").build();

            assertThat(config.getHost()).isEmpty();
            assertThat(config.getScheme()).isEmpty();
            assertThat(config.getKnnSpaceType()).isEmpty();
        }

        @Test
        @DisplayName("同じ項目を連続で指定した場合、最後の指定値であること")
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
        @DisplayName("httpスキームの場合、接続URLがhttp形式であること")
        void testGetConnectionUrlWithHttp() {
            OpenSearchConfig config = new OpenSearchConfig.Builder().scheme("http").host("localhost").port(9200)
                    .build();

            assertThat(config.getConnectionUrl()).isEqualTo("http://localhost:9200");
        }

        @Test
        @DisplayName("httpsスキームの場合、接続URLがhttps形式であること")
        void testGetConnectionUrlWithHttps() {
            OpenSearchConfig config = new OpenSearchConfig.Builder().scheme("https").host("my-host").port(443).build();

            assertThat(config.getConnectionUrl()).isEqualTo("https://my-host:443");
        }

        @Test
        @DisplayName("ホストがIPアドレスの場合、接続URLのホスト部がIPアドレスであること")
        void testGetConnectionUrlWithIpAddress() {
            OpenSearchConfig config = new OpenSearchConfig.Builder().scheme("http").host("192.168.1.100").port(9200)
                    .build();

            assertThat(config.getConnectionUrl()).isEqualTo("http://192.168.1.100:9200");
        }

        @Test
        @DisplayName("ホストがFQDNの場合、接続URLのホスト部がFQDNであること")
        void testGetConnectionUrlWithFqdn() {
            OpenSearchConfig config = new OpenSearchConfig.Builder().scheme("https").host("opensearch.example.com")
                    .port(9200).build();

            assertThat(config.getConnectionUrl()).isEqualTo("https://opensearch.example.com:9200");
        }
    }

    @Nested
    class LoadProperties {
        private final Logger logger = (Logger) LoggerFactory.getLogger(OpenSearchConfig.class);
        private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

        @BeforeEach
        void attachAppender() {
            appender.start();
            logger.addAppender(appender);
        }

        @AfterEach
        void detachAppender() {
            logger.detachAppender(appender);
        }

        private InputStream failingStream() {
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("read failed");
                }
            };
        }

        @Test
        @DisplayName("正常なストリームの場合、値が読み込まれ警告なしであること")
        void testLoadsValuesWithoutWarning() {
            InputStream source = new ByteArrayInputStream(
                    "opensearch.port=9300\n".getBytes(StandardCharsets.ISO_8859_1));

            Properties props = OpenSearchConfig.loadProperties(source);

            assertThat(props.getProperty("opensearch.port")).isEqualTo("9300");
            assertThat(appender.list).isEmpty();
        }

        @Test
        @DisplayName("ファイルが存在しない場合、WARNログが出力され結果が空であること")
        void testMissingFileWarnsAndReturnsEmpty() {
            Properties props = OpenSearchConfig.loadProperties(null);

            assertThat(props).isEmpty();
            assertThat(appender.list).singleElement().satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).contains("not found");
            });
        }

        @Test
        @DisplayName("読み込みに失敗した場合、原因付きのWARNログが出力され結果が空であること")
        void testReadFailureWarnsWithCauseAndReturnsEmpty() {
            Properties props = OpenSearchConfig.loadProperties(failingStream());

            assertThat(props).isEmpty();
            assertThat(appender.list).singleElement().satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getThrowableProxy().getMessage()).isEqualTo("read failed");
            });
        }

        @Test
        @DisplayName("途中で読み込みに失敗した場合、読み込み済みの値も破棄され結果が空であること")
        void testPartialReadFailureDiscardsLoadedValues() {
            InputStream source = new SequenceInputStream(
                    new ByteArrayInputStream("opensearch.port=9300\n".getBytes(StandardCharsets.ISO_8859_1)),
                    failingStream());

            Properties props = OpenSearchConfig.loadProperties(source);

            assertThat(props).isEmpty();
        }
    }

    @Nested
    class FromProperties {
        @Test
        @DisplayName("fromPropertiesの接続設定がopensearch.propertiesの値であること")
        void testFromPropertiesLoadsDefaults() {
            OpenSearchConfig config = OpenSearchConfig.fromProperties();

            // opensearch.properties のデフォルト値を確認
            assertThat(config.getHost()).isEqualTo("localhost");
            assertThat(config.getPort()).isEqualTo(9200);
            assertThat(config.getScheme()).isEqualTo("http");
        }
    }
}
