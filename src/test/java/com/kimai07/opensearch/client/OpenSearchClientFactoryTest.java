package com.kimai07.opensearch.client;

import com.kimai07.opensearch.config.OpenSearchConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class OpenSearchClientFactoryTest {

    @Nested
    class FactoryCreation {
        @Test
        void testCreateWithDefaults() throws IOException {
            try (OpenSearchClientFactory factory = OpenSearchClientFactory.createWithDefaults()) {
                assertThat(factory).isNotNull();
                assertThat(factory.getConfig()).isNotNull();
                assertThat(factory.getConfig().getHost()).isEqualTo("localhost");
                assertThat(factory.getConfig().getPort()).isEqualTo(9200);
            }
        }

        @Test
        void testCreate() throws IOException {
            try (OpenSearchClientFactory factory = OpenSearchClientFactory.create()) {
                assertThat(factory).isNotNull();
                assertThat(factory.getConfig()).isNotNull();
            }
        }

        @Test
        void testCreateWithCustomConfig() throws IOException {
            OpenSearchConfig config = new OpenSearchConfig.Builder()
                    .host("custom-host")
                    .port(9201)
                    .scheme("https")
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
        void testGetClientReturnsSameInstance() throws IOException {
            try (OpenSearchClientFactory factory = OpenSearchClientFactory.createWithDefaults()) {
                var client1 = factory.getClient();
                var client2 = factory.getClient();

                assertThat(client1).isSameAs(client2);
            }
        }

        @Test
        void testGetClientReturnsNonNull() throws IOException {
            try (OpenSearchClientFactory factory = OpenSearchClientFactory.createWithDefaults()) {
                var client = factory.getClient();

                assertThat(client).isNotNull();
            }
        }

        @Test
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
        void testCloseAfterGetClient() throws IOException {
            try (OpenSearchClientFactory factory = OpenSearchClientFactory.createWithDefaults()) {
                factory.getClient(); // クライアント初期化

                // close()が例外なく実行されることを確認
                assertThatCode(factory::close).doesNotThrowAnyException();
            }
        }

        @Test
        void testCloseBeforeGetClient() throws IOException {
            try (OpenSearchClientFactory factory = OpenSearchClientFactory.createWithDefaults()) {
                // クライアント未初期化でclose()しても例外が発生しない
                assertThatCode(factory::close).doesNotThrowAnyException();
            }
        }

        @Test
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
    class ConfigAccess {
        @Test
        void testGetConfigReturnsProvidedConfig() throws IOException {
            OpenSearchConfig config = new OpenSearchConfig.Builder()
                    .host("test-host")
                    .port(9999)
                    .build();

            try (OpenSearchClientFactory factory = new OpenSearchClientFactory(config)) {
                assertThat(factory.getConfig()).isSameAs(config);
            }
        }

        @Test
        void testGetConfigFromStaticFactoryMethod() throws IOException {
            try (OpenSearchClientFactory factory = OpenSearchClientFactory.create()) {
                // fromProperties()で作成された設定が返される
                assertThat(factory.getConfig()).isNotNull();
                assertThat(factory.getConfig().getHost()).isEqualTo("localhost");
            }
        }
    }
}