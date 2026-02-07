package com.kimai07.opensearch.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * OpenSearch接続設定を管理するクラス。
 * <p>
 * このクラスはイミュータブルであり、Builderパターンを使用して構築します。
 * application.propertiesから設定を読み込むか、プログラムで直接設定を指定できます。
 * </p>
 */
public class OpenSearchConfig {

    private final String host;
    private final int port;
    private final String scheme;
    private final int connectionTimeout;
    private final int socketTimeout;
    private final int numberOfShards;
    private final int numberOfReplicas;
    private final int knnDimension;
    private final String knnSpaceType;

    /**
     * Builderから設定を構築するプライベートコンストラクタ。
     *
     * @param builder 設定値を保持する Builder
     */
    private OpenSearchConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.scheme = builder.scheme;
        this.connectionTimeout = builder.connectionTimeout;
        this.socketTimeout = builder.socketTimeout;
        this.numberOfShards = builder.numberOfShards;
        this.numberOfReplicas = builder.numberOfReplicas;
        this.knnDimension = builder.knnDimension;
        this.knnSpaceType = builder.knnSpaceType;
    }

    /**
     * application.propertiesから設定を読み込んでOpenSearchConfigを作成する。
     * <p>
     * プロパティファイルが見つからない場合や読み込みに失敗した場合は、
     * デフォルト値が使用されます。
     * </p>
     *
     * @return application.propertiesの値で初期化されたOpenSearchConfig
     */
    public static OpenSearchConfig fromProperties() {
        Properties props = new Properties();
        try (InputStream is = OpenSearchConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException _) {
            // デフォルト値を使用
        }

        return new Builder()
                .host(props.getProperty("opensearch.host", "localhost"))
                .port(Integer.parseInt(props.getProperty("opensearch.port", "9200")))
                .scheme(props.getProperty("opensearch.scheme", "http"))
                .connectionTimeout(Integer.parseInt(props.getProperty("opensearch.connection.timeout", "5000")))
                .socketTimeout(Integer.parseInt(props.getProperty("opensearch.socket.timeout", "60000")))
                .numberOfShards(Integer.parseInt(props.getProperty("opensearch.index.number_of_shards", "1")))
                .numberOfReplicas(Integer.parseInt(props.getProperty("opensearch.index.number_of_replicas", "0")))
                .knnDimension(Integer.parseInt(props.getProperty("opensearch.knn.dimension", "128")))
                .knnSpaceType(props.getProperty("opensearch.knn.space_type", "l2"))
                .build();
    }

    /**
     * デフォルト設定でOpenSearchConfigを作成する。
     * <p>
     * デフォルト値: host=localhost, port=9200, scheme=http
     * </p>
     *
     * @return デフォルト値で初期化された OpenSearchConfig
     */
    public static OpenSearchConfig defaults() {
        return new Builder().build();
    }

    /**
     * OpenSearchのホスト名を取得する。
     *
     * @return ホスト名
     */
    public String getHost() {
        return host;
    }

    /**
     * OpenSearchのポート番号を取得する。
     *
     * @return ポート番号
     */
    public int getPort() {
        return port;
    }

    /**
     * 接続スキーム（http/https）を取得する。
     *
     * @return 接続スキーム
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * 接続タイムアウト（ミリ秒）を取得する。
     *
     * @return 接続タイムアウト（ミリ秒）
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * ソケットタイムアウト（ミリ秒）を取得する。
     *
     * @return ソケットタイムアウト（ミリ秒）
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * インデックスのシャード数を取得する。
     *
     * @return シャード数
     */
    public int getNumberOfShards() {
        return numberOfShards;
    }

    /**
     * インデックスのレプリカ数を取得する。
     *
     * @return レプリカ数
     */
    public int getNumberOfReplicas() {
        return numberOfReplicas;
    }

    /**
     * k-NNベクトルのデフォルト次元数を取得する。
     *
     * @return ベクトルの次元数
     */
    public int getKnnDimension() {
        return knnDimension;
    }

    /**
     * k-NNの距離計算タイプを取得する。
     * <p>
     * 例: "l2"（ユークリッド距離）, "cosinesimil"（コサイン類似度）
     * </p>
     *
     * @return 距離計算タイプ
     */
    public String getKnnSpaceType() {
        return knnSpaceType;
    }

    /**
     * 接続URLを取得する。
     * <p>
     * フォーマット: {scheme}://{host}:{port}
     * </p>
     *
     * @return 接続 URL 文字列
     */
    public String getConnectionUrl() {
        return scheme + "://" + host + ":" + port;
    }

    /**
     * OpenSearchConfigを構築するためのBuilderクラス。
     * <p>
     * メソッドチェーンで設定値を指定し、{@link #build()}で設定を生成します。
     * </p>
     */
    public static class Builder {
        private String host = "localhost";
        private int port = 9200;
        private String scheme = "http";
        private int connectionTimeout = 5000;
        private int socketTimeout = 60000;
        private int numberOfShards = 1;
        private int numberOfReplicas = 0;
        private int knnDimension = 128;
        private String knnSpaceType = "l2";

        /**
         * OpenSearchのホスト名を設定する。
         *
         * @param host ホスト名
         * @return この Builder
         */
        public Builder host(String host) {
            this.host = host;
            return this;
        }

        /**
         * OpenSearchのポート番号を設定する。
         *
         * @param port ポート番号
         * @return この Builder
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * 接続スキーム（http/https）を設定する。
         *
         * @param scheme 接続スキーム
         * @return この Builder
         */
        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        /**
         * 接続タイムアウト（ミリ秒）を設定する。
         *
         * @param connectionTimeout 接続タイムアウト（ミリ秒）
         * @return この Builder
         */
        public Builder connectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        /**
         * ソケットタイムアウト（ミリ秒）を設定する。
         *
         * @param socketTimeout ソケットタイムアウト（ミリ秒）
         * @return この Builder
         */
        public Builder socketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        /**
         * インデックスのシャード数を設定する。
         *
         * @param numberOfShards シャード数
         * @return この Builder
         */
        public Builder numberOfShards(int numberOfShards) {
            this.numberOfShards = numberOfShards;
            return this;
        }

        /**
         * インデックスのレプリカ数を設定する。
         *
         * @param numberOfReplicas レプリカ数
         * @return この Builder
         */
        public Builder numberOfReplicas(int numberOfReplicas) {
            this.numberOfReplicas = numberOfReplicas;
            return this;
        }

        /**
         * k-NNベクトルのデフォルト次元数を設定する。
         *
         * @param knnDimension ベクトルの次元数
         * @return この Builder
         */
        public Builder knnDimension(int knnDimension) {
            this.knnDimension = knnDimension;
            return this;
        }

        /**
         * k-NNの距離計算タイプを設定する。
         *
         * @param knnSpaceType 距離計算タイプ（例: "l2", "cosinesimil"）
         * @return この Builder
         */
        public Builder knnSpaceType(String knnSpaceType) {
            this.knnSpaceType = knnSpaceType;
            return this;
        }

        /**
         * 設定されたパラメータでOpenSearchConfigを構築する。
         *
         * @return 構築された OpenSearchConfig
         */
        public OpenSearchConfig build() {
            return new OpenSearchConfig(this);
        }
    }
}