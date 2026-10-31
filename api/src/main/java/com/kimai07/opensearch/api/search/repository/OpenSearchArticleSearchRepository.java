package com.kimai07.opensearch.api.search.repository;

import com.kimai07.opensearch.api.config.OpenSearchProperties;
import com.kimai07.opensearch.api.search.error.SearchUnavailableException;
import com.kimai07.opensearch.api.search.model.ArticleHit;
import com.kimai07.opensearch.api.search.model.ArticleSearchQuery;
import com.kimai07.opensearch.api.search.model.ArticleSearchResult;
import com.kimai07.opensearch.search.FullTextSearchService;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * ArticleSearchRepositoryのOpenSearch実装。
 */
@Repository
public class OpenSearchArticleSearchRepository implements ArticleSearchRepository {

    private static final List<String> SEARCH_FIELDS = List.of("title", "content");
    private static final String TIE_BREAKER_FIELD = "article_id";

    private final FullTextSearchService searchService;
    private final String indexName;

    public OpenSearchArticleSearchRepository(FullTextSearchService searchService, OpenSearchProperties properties) {
        this.searchService = searchService;
        this.indexName = properties.index();
    }

    @Override
    public ArticleSearchResult search(ArticleSearchQuery query) {
        try {
            SearchResponse<ArticleDocument> response = searchService.multiMatchWithHighlight(indexName, SEARCH_FIELDS,
                    query.text(), query.size(), TIE_BREAKER_FIELD, ArticleDocument.class);
            return toResult(response);
        } catch (IOException | OpenSearchException e) {
            throw new SearchUnavailableException("Failed to search index: " + indexName, e);
        }
    }

    private ArticleSearchResult toResult(SearchResponse<ArticleDocument> response) {
        long total = Objects.requireNonNull(response.hits().total()).value();
        List<ArticleHit> hits = response.hits().hits().stream().map(this::toHit).toList();
        return new ArticleSearchResult(total, hits);
    }

    private ArticleHit toHit(Hit<ArticleDocument> hit) {
        ArticleDocument document = Objects.requireNonNull(hit.source());
        return new ArticleHit(hit.id(), hit.score(), document.title(), document.content(), document.category(),
                hit.highlight());
    }
}
