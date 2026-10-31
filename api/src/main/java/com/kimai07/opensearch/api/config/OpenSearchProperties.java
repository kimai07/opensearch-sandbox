package com.kimai07.opensearch.api.config;

import com.kimai07.opensearch.config.OpenSearchConfig;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * OpenSearchの接続設定。
 *
 * @param host
 *            ホスト名
 * @param port
 *            ポート番号
 * @param scheme
 *            接続スキーム
 * @param connectionTimeout
 *            接続タイムアウト（ミリ秒）
 * @param socketTimeout
 *            ソケットタイムアウト（ミリ秒）
 * @param index
 *            検索対象のインデックス名
 */
@Validated
@ConfigurationProperties(prefix = "opensearch")
public record OpenSearchProperties(@NotBlank String host, @Min(1) @Max(65535) int port, @NotBlank String scheme,
        @Min(0) int connectionTimeout, @Min(0) int socketTimeout, @NotBlank String index) {

    /**
     * coreモジュールの接続設定に変換する。
     *
     * @return 接続設定
     */
    public OpenSearchConfig toConfig() {
        return new OpenSearchConfig.Builder().host(host).port(port).scheme(scheme).connectionTimeout(connectionTimeout)
                .socketTimeout(socketTimeout).build();
    }
}
