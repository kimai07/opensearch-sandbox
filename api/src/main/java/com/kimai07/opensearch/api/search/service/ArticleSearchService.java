package com.kimai07.opensearch.api.search.service;

import com.kimai07.opensearch.api.search.model.ArticleSearchQuery;
import com.kimai07.opensearch.api.search.model.ArticleSearchResult;
import com.kimai07.opensearch.api.search.repository.ArticleSearchRepository;
import org.springframework.stereotype.Service;

/**
 * 記事の検索を行うサービス。
 */
@Service
public class ArticleSearchService {

    private final ArticleSearchRepository repository;

    public ArticleSearchService(ArticleSearchRepository repository) {
        this.repository = repository;
    }

    /**
     * 記事を検索する。
     *
     * @param text
     *            検索文字列
     * @param size
     *            取得する最大件数
     *
     * @return 検索結果
     */
    public ArticleSearchResult search(String text, int size) {
        return repository.search(new ArticleSearchQuery(text, size));
    }
}
