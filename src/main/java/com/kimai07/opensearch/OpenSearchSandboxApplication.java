package com.kimai07.opensearch;

import com.kimai07.opensearch.examples.DemoRunner;

/**
 * OpenSearch Sandboxアプリケーションのエントリーポイント。
 * <p>
 * このクラスはアプリケーションのメインクラスとして機能し、 {@link DemoRunner}を起動してOpenSearchの各種機能をデモンストレーションします。
 * </p>
 *
 * @see DemoRunner
 */

public class OpenSearchSandboxApplication {

    private OpenSearchSandboxApplication() {
    }

    /**
     * アプリケーションのエントリーポイント。
     * <p>
     * DemoRunnerを起動してデモンストレーションを実行します。
     * </p>
     *
     * @param args
     *            コマンドライン引数（DemoRunnerに渡されます）
     */
    static void main(String[] args) {
        DemoRunner.main(args);
    }
}