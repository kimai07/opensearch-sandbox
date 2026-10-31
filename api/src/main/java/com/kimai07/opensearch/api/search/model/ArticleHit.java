package com.kimai07.opensearch.api.search.model;

import java.util.List;
import java.util.Map;

/**
 * 検索にヒットした記事。
 *
 * @param id
 *            ドキュメントID
 * @param score
 *            関連度スコア
 * @param title
 *            タイトル
 * @param content
 *            本文
 * @param category
 *            カテゴリ
 * @param highlights
 *            フィールド名をキーとするハイライト
 */
public record ArticleHit(String id, Double score, String title, String content, String category,
        Map<String, List<String>> highlights) {
}
