package com.kimai07.opensearch.search;

import com.kimai07.opensearch.client.SearchClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FullTextSearchServiceTest {

    private SearchClient mockClient;

    private FullTextSearchService service;

    @BeforeEach
    void setUp() {
        mockClient = mock(SearchClient.class);
        service = new FullTextSearchService(mockClient);
    }

    @Nested
    class ExtractDocuments {
        @Test
        void testExtractDocumentsFromEmptyResponse() {
            // 空のレスポンスを作成
            SearchResponse<String> response = createMockSearchResponse(List.of());

            List<String> documents = service.extractDocuments(response);

            assertThat(documents).isEmpty();
        }

        @Test
        void testExtractDocumentsFromResponseWithHits() {
            // ヒットありのレスポンスを作成
            List<Hit<String>> hits = List.of(createMockHit("1", "Document 1", 1.0),
                    createMockHit("2", "Document 2", 0.9), createMockHit("3", "Document 3", 0.8));
            SearchResponse<String> response = createMockSearchResponse(hits);

            List<String> documents = service.extractDocuments(response);

            assertThat(documents).hasSize(3).containsExactly("Document 1", "Document 2", "Document 3");
        }

        @Test
        void testExtractDocumentsWithNullSource() {
            // source が null のヒットを含むレスポンス
            List<Hit<String>> hits = List.of(createMockHit("1", null, 1.0), createMockHit("2", "Document 2", 0.9));
            SearchResponse<String> response = createMockSearchResponse(hits);

            List<String> documents = service.extractDocuments(response);

            assertThat(documents).hasSize(2).containsExactly(null, "Document 2");
        }
    }

    @Nested
    class ExtractHighlights {
        @Test
        void testExtractHighlightsFromHit() {
            Map<String, List<String>> highlights = Map.of("content", List.of("<em>OpenSearch</em> is great"));
            Hit<String> hit = createMockHitWithHighlight("1", "content", 1.0, highlights);

            Map<String, List<String>> extracted = service.extractHighlights(hit);

            assertThat(extracted).containsKey("content");
            assertThat(extracted.get("content")).containsExactly("<em>OpenSearch</em> is great");
        }

        @Test
        void testExtractHighlightsWithMultipleFields() {
            Map<String, List<String>> highlights = Map.of("title", List.of("<em>Search</em> Guide"), "content",
                    List.of("<em>Search</em> is powerful", "Learn <em>Search</em>"));
            Hit<String> hit = createMockHitWithHighlight("1", "content", 1.0, highlights);

            Map<String, List<String>> extracted = service.extractHighlights(hit);

            assertThat(extracted).hasSize(2);
            assertThat(extracted.get("title")).hasSize(1);
            assertThat(extracted.get("content")).hasSize(2);
        }

        @Test
        void testExtractHighlightsWhenEmpty() {
            Hit<String> hit = createMockHitWithHighlight("1", "content", 1.0, Map.of());

            Map<String, List<String>> extracted = service.extractHighlights(hit);

            assertThat(extracted).isEmpty();
        }
    }

    @Nested
    class MatchQuery {
        @Test
        void testMatchQueryThrowsIOExceptionOnError() throws IOException {
            when(mockClient.search(any(SearchRequest.class), eq(String.class)))
                    .thenThrow(new IOException("Connection failed"));

            assertThatThrownBy(() -> service.matchQuery("test-index", "content", "search", String.class))
                    .isInstanceOf(IOException.class).hasMessage("Connection failed");
        }

        @Test
        void testMatchQueryReturnsResponse() throws IOException {
            SearchResponse<String> mockResponse = createMockSearchResponse(
                    List.of(createMockHit("1", "Result 1", 1.0)));
            when(mockClient.search(any(SearchRequest.class), eq(String.class))).thenReturn(mockResponse);

            SearchResponse<String> response = service.matchQuery("test-index", "content", "search", String.class);

            assertThat(response).isNotNull();
            assertThat(response.hits().hits()).hasSize(1);
        }
    }

    @Nested
    class BoolQuery {
        @Test
        void testBoolQueryWithAllNullConditions() throws IOException {
            SearchResponse<String> mockResponse = createMockSearchResponse(List.of());
            when(mockClient.search(any(SearchRequest.class), eq(String.class))).thenReturn(mockResponse);

            SearchResponse<String> response = service.boolQuery("test-index", null, null, null, String.class);

            assertThat(response).isNotNull();
        }

        @Test
        void testBoolQueryWithEmptyConditions() throws IOException {
            SearchResponse<String> mockResponse = createMockSearchResponse(List.of());
            when(mockClient.search(any(SearchRequest.class), eq(String.class))).thenReturn(mockResponse);

            SearchResponse<String> response = service.boolQuery("test-index", List.of(), List.of(), List.of(),
                    String.class);

            assertThat(response).isNotNull();
        }

        @Test
        void testBoolQueryThrowsIOExceptionOnError() throws IOException {
            when(mockClient.search(any(SearchRequest.class), eq(String.class)))
                    .thenThrow(new IOException("Search failed"));

            List<Query> must = List.of(Query.of(q -> q.matchAll(m -> m)));

            assertThatThrownBy(() -> service.boolQuery("test-index", must, null, null, String.class))
                    .isInstanceOf(IOException.class).hasMessage("Search failed");
        }
    }

    @Nested
    class FuzzyQuery {
        @Test
        void testFuzzyQueryReturnsResponse() throws IOException {
            SearchResponse<String> mockResponse = createMockSearchResponse(
                    List.of(createMockHit("1", "OpenSearch", 0.9)));
            when(mockClient.search(any(SearchRequest.class), eq(String.class))).thenReturn(mockResponse);

            SearchResponse<String> response = service.fuzzyQuery("test-index", "content", "OpenSearch", "2",
                    String.class);

            assertThat(response).isNotNull();
            assertThat(response.hits().hits()).hasSize(1);
        }

        @Test
        void testFuzzyQueryThrowsIOExceptionOnError() throws IOException {
            when(mockClient.search(any(SearchRequest.class), eq(String.class)))
                    .thenThrow(new IOException("Fuzzy search failed"));

            assertThatThrownBy(() -> service.fuzzyQuery("test-index", "content", "test", "AUTO", String.class))
                    .isInstanceOf(IOException.class).hasMessage("Fuzzy search failed");
        }
    }

    @Nested
    class MultiMatchQuery {
        @Test
        void testMultiMatchQueryReturnsResponse() throws IOException {
            SearchResponse<String> mockResponse = createMockSearchResponse(List.of(createMockHit("1", "Result", 1.0)));
            when(mockClient.search(any(SearchRequest.class), eq(String.class))).thenReturn(mockResponse);

            SearchResponse<String> response = service.multiMatchQuery("test-index", List.of("title", "content"),
                    "search", String.class);

            assertThat(response).isNotNull();
        }

        @Test
        void testMultiMatchQueryWithEmptyFields() throws IOException {
            SearchResponse<String> mockResponse = createMockSearchResponse(List.of());
            when(mockClient.search(any(SearchRequest.class), eq(String.class))).thenReturn(mockResponse);

            SearchResponse<String> response = service.multiMatchQuery("test-index", List.of(), "search", String.class);

            assertThat(response).isNotNull();
        }
    }

    @Nested
    class PhraseMatchQuery {
        @Test
        void testPhraseMatchQueryReturnsResponse() throws IOException {
            SearchResponse<String> mockResponse = createMockSearchResponse(
                    List.of(createMockHit("1", "Exact phrase match", 1.0)));
            when(mockClient.search(any(SearchRequest.class), eq(String.class))).thenReturn(mockResponse);

            SearchResponse<String> response = service.phraseMatchQuery("test-index", "content", "exact phrase",
                    String.class);

            assertThat(response).isNotNull();
        }
    }

    @Nested
    class WildcardQuery {
        @Test
        void testWildcardQueryReturnsResponse() throws IOException {
            SearchResponse<String> mockResponse = createMockSearchResponse(
                    List.of(createMockHit("1", "Testing", 1.0), createMockHit("2", "Tested", 0.9)));
            when(mockClient.search(any(SearchRequest.class), eq(String.class))).thenReturn(mockResponse);

            SearchResponse<String> response = service.wildcardQuery("test-index", "content", "test*", String.class);

            assertThat(response).isNotNull();
            assertThat(response.hits().hits()).hasSize(2);
        }
    }

    @Nested
    class SearchWithHighlight {
        @Test
        void testSearchWithHighlightReturnsResponse() throws IOException {
            SearchResponse<String> mockResponse = createMockSearchResponse(
                    List.of(createMockHit("1", "Result with highlight", 1.0)));
            when(mockClient.search(any(SearchRequest.class), eq(String.class))).thenReturn(mockResponse);

            SearchResponse<String> response = service.searchWithHighlight("test-index", "content", "highlight",
                    String.class);

            assertThat(response).isNotNull();
        }
    }

    // ヘルパーメソッド
    @SuppressWarnings("unchecked")
    private <T> SearchResponse<T> createMockSearchResponse(List<Hit<T>> hits) {
        TotalHits totalHits = TotalHits.of(t -> t.value(hits.size()).relation(TotalHitsRelation.Eq));

        HitsMetadata<T> hitsMetadata = mock(HitsMetadata.class);
        when(hitsMetadata.total()).thenReturn(totalHits);
        when(hitsMetadata.hits()).thenReturn(hits);

        SearchResponse<T> response = mock(SearchResponse.class);
        when(response.took()).thenReturn(10L);
        when(response.timedOut()).thenReturn(false);
        when(response.hits()).thenReturn(hitsMetadata);

        return response;
    }

    private <T> Hit<T> createMockHit(String id, T source, double score) {
        return createMockHitWithHighlight(id, source, score, Map.of());
    }

    private <T> Hit<T> createMockHitWithHighlight(String id, T source, double score,
            Map<String, List<String>> highlight) {
        return Hit.of(h -> h.index("test-index").id(id).score(score).source(source).highlight(highlight));
    }
}