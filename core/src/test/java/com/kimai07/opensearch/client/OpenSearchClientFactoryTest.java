package com.kimai07.opensearch.client;

import com.kimai07.opensearch.config.OpenSearchConfig;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class OpenSearchClientFactoryTest {

    @Nested
    class FactoryCreation {
        @Test
        @DisplayName("デフォルト設定の場合、接続先がlocalhost:9200であること")
        void testCreateWithDefaults() throws IOException {
            try (OpenSearchClientFactory factory = OpenSearchClientFactory.createWithDefaults()) {
                assertThat(factory).isNotNull();
                assertThat(factory.getConfig()).isNotNull();
                assertThat(factory.getConfig().getHost()).isEqualTo("localhost");
                assertThat(factory.getConfig().getPort()).isEqualTo(9200);
            }
        }

        @Test
        @DisplayName("opensearch.propertiesから生成した場合、設定が取得可能であること")
        void testCreate() throws IOException {
            try (OpenSearchClientFactory factory = OpenSearchClientFactory.create()) {
                assertThat(factory).isNotNull();
                assertThat(factory.getConfig()).isNotNull();
            }
        }

        @Test
        @DisplayName("カスタム設定の場合、接続先が指定値であること")
        void testCreateWithCustomConfig() throws IOException {
            OpenSearchConfig config = new OpenSearchConfig.Builder().host("custom-host").port(9201).scheme("https")
                    .build();

            try (OpenSearchClientFactory factory = new OpenSearchClientFactory(config)) {
                assertThat(factory.getConfig().getHost()).isEqualTo("custom-host");
                assertThat(factory.getConfig().getPort()).isEqualTo(9201);
                assertThat(factory.getConfig().getScheme()).isEqualTo("https");
            }
        }
    }

    @Nested
    class ClientManagement {
        @Test
        @DisplayName("複数回取得した場合、同一インスタンスであること")
        void testGetClientReturnsSameInstance() throws IOException {
            try (OpenSearchClientFactory factory = OpenSearchClientFactory.createWithDefaults()) {
                var client1 = factory.getClient();
                var client2 = factory.getClient();

                assertThat(client1).isSameAs(client2);
            }
        }

        @Test
        @DisplayName("取得したクライアントがnull以外であること")
        void testGetClientReturnsNonNull() throws IOException {
            try (OpenSearchClientFactory factory = OpenSearchClientFactory.createWithDefaults()) {
                var client = factory.getClient();

                assertThat(client).isNotNull();
            }
        }

        @Test
        @DisplayName("複数スレッドから同時に取得した場合、全スレッドで同一インスタンスであること")
        void testGetClientIsThreadSafe() throws InterruptedException, IOException {
            try (OpenSearchClientFactory factory = OpenSearchClientFactory.createWithDefaults()) {
                var clients = new java.util.concurrent.CopyOnWriteArrayList<org.opensearch.client.opensearch.OpenSearchClient>();

                // 複数スレッドから同時にgetClient()を呼び出す
                Thread[] threads = new Thread[10];
                for (int i = 0; i < threads.length; i++) {
                    threads[i] = new Thread(() -> clients.add(factory.getClient()));
                    threads[i].start();
                }

                for (Thread thread : threads) {
                    thread.join();
                }

                // すべてのスレッドが同じインスタンスを取得したことを確認
                assertThat(clients).hasSize(10);
                var firstClient = clients.getFirst();
                assertThat(clients).allMatch(c -> c == firstClient);
            }
        }
    }

    @Nested
    class ResourceManagement {
        @Test
        @DisplayName("クライアント初期化済みの場合、closeが例外なく実行可能であること")
        void testCloseAfterGetClient() throws IOException {
            try (OpenSearchClientFactory factory = OpenSearchClientFactory.createWithDefaults()) {
                factory.getClient(); // クライアント初期化

                // close()が例外なく実行されることを確認
                assertThatCode(factory::close).doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("クライアント未初期化の場合、closeが例外なく実行可能であること")
        void testCloseBeforeGetClient() throws IOException {
            try (OpenSearchClientFactory factory = OpenSearchClientFactory.createWithDefaults()) {
                // クライアント未初期化でclose()しても例外が発生しない
                assertThatCode(factory::close).doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("複数回closeした場合、2回目以降も例外なく実行可能であること")
        void testMultipleCloseCallsAreSafe() throws IOException {
            try (OpenSearchClientFactory factory = OpenSearchClientFactory.createWithDefaults()) {
                factory.getClient();

                // 複数回close()を呼び出しても安全
                factory.close();
                assertThatCode(factory::close).doesNotThrowAnyException();
            }
        }
    }

    @Nested
    class AfterClose {
        @Test
        @DisplayName("初期化後にクローズした場合、getClientの例外がIllegalStateExceptionであること")
        void testGetClientAfterCloseThrows() throws IOException {
            OpenSearchClientFactory factory = OpenSearchClientFactory.createWithDefaults();
            factory.getClient();
            factory.close();

            assertThatThrownBy(factory::getClient).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("未初期化のままクローズした場合、getClientの例外がIllegalStateExceptionであること")
        void testGetClientAfterCloseWithoutInitializationThrows() throws IOException {
            OpenSearchClientFactory factory = OpenSearchClientFactory.createWithDefaults();
            factory.close();

            assertThatThrownBy(factory::getClient).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class Timeouts {
        private final OpenSearchClientFactory factory = new OpenSearchClientFactory(
                new OpenSearchConfig.Builder().connectionTimeout(1500).socketTimeout(2500).build());

        @Test
        @DisplayName("接続設定の接続タイムアウトとソケットタイムアウトが設定値であること")
        void testConfigureConnectionAppliesConfiguredTimeouts() {
            ConnectionConfig connectionConfig = factory.configureConnection(ConnectionConfig.custom()).build();

            assertThat(connectionConfig.getConnectTimeout()).isEqualTo(Timeout.ofMilliseconds(1500));
            assertThat(connectionConfig.getSocketTimeout()).isEqualTo(Timeout.ofMilliseconds(2500));
        }

        @Test
        @DisplayName("レスポンス待ちのタイムアウトがソケットタイムアウトの設定値であること")
        void testConfigureRequestAppliesSocketTimeoutToResponseWait() {
            RequestConfig requestConfig = factory.configureRequest(RequestConfig.custom()).build();

            assertThat(requestConfig.getResponseTimeout()).isEqualTo(Timeout.ofMilliseconds(2500));
        }

        @Test
        @DisplayName("サーバーが無応答な場合、設定したタイムアウトで打ち切られ接続テストの結果がfalseであること")
        void testSocketTimeoutAbortsWhenServerDoesNotRespond() throws IOException {
            // 接続は受け付けるが応答を返さないサーバー
            try (ServerSocket silentServer = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
                OpenSearchConfig config = new OpenSearchConfig.Builder().host("127.0.0.1")
                        .port(silentServer.getLocalPort()).socketTimeout(300).build();

                try (OpenSearchClientFactory silentFactory = new OpenSearchClientFactory(config)) {
                    // ライブラリ既定の30秒ではなく、設定値で打ち切られることを確認
                    assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                        assertThat(silentFactory.testConnection()).isFalse();
                    });
                }
            }
        }
    }

    @Nested
    class ConfigAccess {
        @Test
        @DisplayName("取得した設定がコンストラクタで渡した設定と同一であること")
        void testGetConfigReturnsProvidedConfig() throws IOException {
            OpenSearchConfig config = new OpenSearchConfig.Builder().host("test-host").port(9999).build();

            try (OpenSearchClientFactory factory = new OpenSearchClientFactory(config)) {
                assertThat(factory.getConfig()).isSameAs(config);
            }
        }

        @Test
        @DisplayName("staticファクトリで生成した場合、設定がopensearch.propertiesの値であること")
        void testGetConfigFromStaticFactoryMethod() throws IOException {
            try (OpenSearchClientFactory factory = OpenSearchClientFactory.create()) {
                // fromProperties()で作成された設定が返される
                assertThat(factory.getConfig()).isNotNull();
                assertThat(factory.getConfig().getHost()).isEqualTo("localhost");
            }
        }
    }
}
