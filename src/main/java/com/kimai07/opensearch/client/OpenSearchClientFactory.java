package com.kimai07.opensearch.client;

import com.kimai07.opensearch.config.OpenSearchConfig;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * OpenSearchClientのファクトリクラス。
 * <p>
 * OpenSearchへの接続を管理し、クライアントインスタンスを提供します。 シングルトンパターンでクライアントを管理し、リソースの効率的な利用を実現します。
 * </p>
 * <p>
 * 使用後は必ず{@link #close()}を呼び出してリソースを解放してください。
 * </p>
 *
 * @see OpenSearchConfig
 * @see OpenSearchClient
 */
public class OpenSearchClientFactory implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchClientFactory.class);

    private final OpenSearchConfig config;
    private OpenSearchClient client;
    private org.opensearch.client.transport.OpenSearchTransport transport;
    private boolean closed;

    /**
     * 指定された設定でファクトリを構築する。
     *
     * @param config
     *            OpenSearch 接続設定
     */
    public OpenSearchClientFactory(OpenSearchConfig config) {
        this.config = config;
    }

    /**
     * application.propertiesから設定を読み込んでファクトリを作成する。
     *
     * @return 設定ファイルから初期化された OpenSearchClientFactory
     */
    public static OpenSearchClientFactory create() {
        return new OpenSearchClientFactory(OpenSearchConfig.fromProperties());
    }

    /**
     * デフォルト設定でファクトリを作成する。
     * <p>
     * localhost:9200に接続するデフォルト設定が使用されます。
     * </p>
     *
     * @return デフォルト設定で初期化された OpenSearchClientFactory
     */
    public static OpenSearchClientFactory createWithDefaults() {
        return new OpenSearchClientFactory(OpenSearchConfig.defaults());
    }

    /**
     * OpenSearchClientを取得する。
     * <p>
     * クライアントは遅延初期化され、同じインスタンスが再利用されます（シングルトン）。 このメソッドはスレッドセーフです。
     * </p>
     *
     * @return OpenSearchClient インスタンス
     *
     * @throws IllegalStateException
     *             ファクトリがクローズ済みの場合
     */
    public synchronized OpenSearchClient getClient() {
        if (closed) {
            throw new IllegalStateException("OpenSearchClientFactory is already closed");
        }
        if (client == null) {
            client = createClient();
        }
        return client;
    }

    /**
     * 現在の設定を取得する。
     *
     * @return OpenSearch 接続設定
     */
    public OpenSearchConfig getConfig() {
        return config;
    }

    /**
     * OpenSearchClientインスタンスを作成する。
     *
     * @return 新しい OpenSearchClient インスタンス
     */
    private OpenSearchClient createClient() {
        logger.info("Creating OpenSearch client for {}:{}", config.getHost(), config.getPort());

        HttpHost host = new HttpHost(config.getScheme(), config.getHost(), config.getPort());

        transport = ApacheHttpClient5TransportBuilder.builder(host)
                .setConnectionConfigCallback(this::configureConnection).setRequestConfigCallback(this::configureRequest)
                .build();

        return new OpenSearchClient(transport);
    }

    /**
     * 接続タイムアウトとソケットタイムアウトを接続設定に反映する。
     *
     * @param builder
     *            設定対象の ConnectionConfig.Builder
     *
     * @return タイムアウトを反映した Builder
     */
    ConnectionConfig.Builder configureConnection(ConnectionConfig.Builder builder) {
        return builder.setConnectTimeout(config.getConnectionTimeout(), TimeUnit.MILLISECONDS)
                .setSocketTimeout(config.getSocketTimeout(), TimeUnit.MILLISECONDS);
    }

    /**
     * ソケットタイムアウトをレスポンス待ちのタイムアウトに反映する。
     * <p>
     * トランスポートの既定値（30秒）を上書きしないと、応答待ちに設定値が適用されません。
     * </p>
     *
     * @param builder
     *            設定対象の RequestConfig.Builder
     *
     * @return タイムアウトを反映した Builder
     */
    RequestConfig.Builder configureRequest(RequestConfig.Builder builder) {
        return builder.setResponseTimeout(config.getSocketTimeout(), TimeUnit.MILLISECONDS);
    }

    /**
     * OpenSearchへの接続をテストする。
     * <p>
     * クラスター情報を取得し、接続が正常に確立できるか確認します。
     * </p>
     *
     * @return 接続成功の場合 true、失敗の場合 false
     */
    public boolean testConnection() {
        try {
            var info = getClient().info();
            logger.info("Connected to OpenSearch cluster: {}, version: {}", info.clusterName(),
                    info.version().number());
            return true;
        } catch (IOException e) {
            logger.error("Failed to connect to OpenSearch: {}", e.getMessage());
            return false;
        }
    }

    /**
     * リソースを解放し、接続をクローズする。
     * <p>
     * このメソッドを呼び出した後は、このファクトリから取得したクライアントは使用できず、 {@link #getClient()}は例外をスローします。複数回呼び出しても安全です。
     * </p>
     *
     * @throws IOException
     *             クローズ中にエラーが発生した場合
     */
    @Override
    public synchronized void close() throws IOException {
        closed = true;
        client = null;
        if (transport != null) {
            logger.info("Closing OpenSearch client");
            try {
                transport.close();
            } finally {
                transport = null;
            }
        }
    }
}
