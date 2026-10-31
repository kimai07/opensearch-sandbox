package com.kimai07.opensearch.api.search.repository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * OpenSearchに保存された記事ドキュメント。
 *
 * @param title
 *            タイトル
 * @param content
 *            本文
 * @param category
 *            カテゴリ
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArticleDocument(String title, String content, String category) {
}
