package com.kimai07.opensearch.api.search.regression;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * リグレッションテストの1ケース。
 *
 * @param name
 *            ケース名（期待値のファイル名）
 * @param q
 *            検索文字列
 * @param size
 *            取得する最大件数（nullの場合は指定しない）
 */
record RegressionCase(String name, String q, Integer size) {

    /**
     * クエリ文字列を作成する。
     *
     * @return URLエンコード済みのクエリ文字列
     */
    String toQueryString() {
        String encoded = "q=" + URLEncoder.encode(q, StandardCharsets.UTF_8);
        return size == null ? encoded : encoded + "&size=" + size;
    }

    @Override
    public String toString() {
        return name;
    }
}
