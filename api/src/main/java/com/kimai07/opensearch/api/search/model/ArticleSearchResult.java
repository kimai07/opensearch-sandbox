package com.kimai07.opensearch.api.search.model;

import java.util.List;

/**
 * 記事の検索結果。
 *
 * @param total
 *            ヒットした総件数
 * @param hits
 *            ヒットした記事
 */
public record ArticleSearchResult(long total, List<ArticleHit> hits) {
}
