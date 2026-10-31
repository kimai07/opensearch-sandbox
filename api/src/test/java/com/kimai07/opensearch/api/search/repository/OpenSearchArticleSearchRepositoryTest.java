package com.kimai07.opensearch.api.search.repository;

import com.kimai07.opensearch.api.config.OpenSearchProperties;
import com.kimai07.opensearch.api.search.error.SearchUnavailableException;
import com.kimai07.opensearch.api.search.model.ArticleHit;
import com.kimai07.opensearch.api.search.model.ArticleSearchQuery;
import com.kimai07.opensearch.api.search.model.ArticleSearchResult;
import com.kimai07.opensearch.search.FullTextSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.HitsMetadata;
import org.opensearch.client.opensearch.core.search.TotalHits;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenSearchArticleSearchRepositoryTest {

    private static final ArticleSearchQuery QUERY = new ArticleSearchQuery("OpenSearch", 5);

    private final FullTextSearchService searchService = mock(FullTextSearchService.class);
    private final OpenSearchArticleSearchRepository repository = new OpenSearchArticleSearchRepository(searchService,
            new OpenSearchProperties("localhost", 9200, "http", 5000, 60000, "articles"));

    @Test
    @DisplayName("検索条件を指定した場合、タイトルと本文を対象に設定のインデックスへ検索する内容であること")
    void testSearchCallsServiceWithQuery() throws IOException {
        givenSearchReturns(List.of());

        repository.search(QUERY);

        verify(searchService).multiMatchWithHighlight("articles", List.of("title", "content"), "OpenSearch", 5,
                "article_id", ArticleDocument.class);
    }

    @Test
    @DisplayName("ヒットありの場合、結果がドキュメントとハイライトの値であること")
    void testSearchConvertsHits() throws IOException {
        Hit<ArticleDocument> hit = Hit.of(h -> h.index("articles").id("1").score(1.5)
                .source(new ArticleDocument("Title", "Content", "technology"))
                .highlight(Map.of("title", List.of("<em>Title</em>"))));
        givenSearchReturns(List.of(hit));

        ArticleSearchResult result = repository.search(QUERY);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.hits()).containsExactly(
                new ArticleHit("1", 1.5, "Title", "Content", "technology", Map.of("title", List.of("<em>Title</em>"))));
    }

    @Test
    @DisplayName("通信エラーが発生した場合、例外がSearchUnavailableExceptionであること")
    void testSearchWrapsIOException() throws IOException {
        when(searchService.multiMatchWithHighlight(anyString(), anyList(), anyString(), anyInt(), anyString(),
                eq(ArticleDocument.class))).thenThrow(new IOException("Connection failed"));

        assertThatThrownBy(() -> repository.search(QUERY)).isInstanceOf(SearchUnavailableException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("OpenSearchがエラーを返した場合、例外がSearchUnavailableExceptionであること")
    void testSearchWrapsOpenSearchException() throws IOException {
        ErrorResponse error = ErrorResponse.of(e -> e.status(500)
                .error(ErrorCause.of(c -> c.type("search_phase_execution_exception").reason("failed"))));
        when(searchService.multiMatchWithHighlight(anyString(), anyList(), anyString(), anyInt(), anyString(),
                eq(ArticleDocument.class))).thenThrow(new OpenSearchException(error));

        assertThatThrownBy(() -> repository.search(QUERY)).isInstanceOf(SearchUnavailableException.class)
                .hasCauseInstanceOf(OpenSearchException.class);
    }

    @SuppressWarnings("unchecked")
    private void givenSearchReturns(List<Hit<ArticleDocument>> hits) throws IOException {
        TotalHits totalHits = TotalHits.of(t -> t.value(hits.size()).relation(TotalHitsRelation.Eq));
        HitsMetadata<ArticleDocument> hitsMetadata = mock(HitsMetadata.class);
        when(hitsMetadata.total()).thenReturn(totalHits);
        when(hitsMetadata.hits()).thenReturn(hits);

        SearchResponse<ArticleDocument> response = mock(SearchResponse.class);
        when(response.hits()).thenReturn(hitsMetadata);

        when(searchService.multiMatchWithHighlight(anyString(), anyList(), anyString(), anyInt(), anyString(),
                eq(ArticleDocument.class))).thenReturn(response);
    }
}
