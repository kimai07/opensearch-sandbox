package com.kimai07.opensearch.api.search.model;

/**
 * 記事の検索条件。
 *
 * @param text
 *            検索文字列
 * @param size
 *            取得する最大件数
 */
public record ArticleSearchQuery(String text, int size) {
}
