package com.kimai07.opensearch.api.search.service;

import com.kimai07.opensearch.api.search.model.ArticleSearchQuery;
import com.kimai07.opensearch.api.search.model.ArticleSearchResult;
import com.kimai07.opensearch.api.search.repository.ArticleSearchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArticleSearchServiceTest {

    private final ArticleSearchRepository repository = mock(ArticleSearchRepository.class);
    private final ArticleSearchService service = new ArticleSearchService(repository);

    @Test
    @DisplayName("検索条件を指定した場合、結果がrepositoryの検索結果であること")
    void testSearchReturnsRepositoryResult() {
        ArticleSearchResult expected = new ArticleSearchResult(0, List.of());
        when(repository.search(new ArticleSearchQuery("OpenSearch", 5))).thenReturn(expected);

        ArticleSearchResult result = service.search("OpenSearch", 5);

        assertThat(result).isSameAs(expected);
    }
}
