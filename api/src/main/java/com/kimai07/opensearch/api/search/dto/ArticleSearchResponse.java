package com.kimai07.opensearch.api.search.dto;

import com.kimai07.opensearch.api.search.model.ArticleSearchResult;

import java.util.List;

/**
 * 記事検索のレスポンス。
 *
 * @param total
 *            ヒットした総件数
 * @param hits
 *            ヒットした記事
 */
public record ArticleSearchResponse(long total, List<ArticleHitResponse> hits) {

    /**
     * 検索結果からレスポンスを作成する。
     *
     * @param result
     *            検索結果
     *
     * @return レスポンス
     */
    public static ArticleSearchResponse from(ArticleSearchResult result) {
        return new ArticleSearchResponse(result.total(), result.hits().stream().map(ArticleHitResponse::from).toList());
    }
}
