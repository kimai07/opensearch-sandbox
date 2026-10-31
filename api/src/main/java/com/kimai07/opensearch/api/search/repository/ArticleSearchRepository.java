package com.kimai07.opensearch.api.search.repository;

import com.kimai07.opensearch.api.search.model.ArticleSearchQuery;
import com.kimai07.opensearch.api.search.model.ArticleSearchResult;

/**
 * 記事の検索を行うリポジトリ。
 */
public interface ArticleSearchRepository {

    /**
     * 記事を検索する。
     *
     * @param query
     *            検索条件
     *
     * @return 検索結果
     *
     * @throws com.kimai07.opensearch.api.search.error.SearchUnavailableException
     *             検索エンジンを利用できない場合
     */
    ArticleSearchResult search(ArticleSearchQuery query);
}
