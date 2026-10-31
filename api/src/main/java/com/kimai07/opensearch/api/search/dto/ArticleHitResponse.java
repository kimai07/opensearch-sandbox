package com.kimai07.opensearch.api.search.dto;

import com.kimai07.opensearch.api.search.model.ArticleHit;

import java.util.List;
import java.util.Map;

/**
 * 検索にヒットした記事のレスポンス。
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
public record ArticleHitResponse(String id, Double score, String title, String content, String category,
        Map<String, List<String>> highlights) {

    static ArticleHitResponse from(ArticleHit hit) {
        return new ArticleHitResponse(hit.id(), hit.score(), hit.title(), hit.content(), hit.category(),
                hit.highlights());
    }
}
