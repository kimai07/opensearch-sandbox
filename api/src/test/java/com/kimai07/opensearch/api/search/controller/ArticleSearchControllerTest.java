package com.kimai07.opensearch.api.search.controller;

import com.kimai07.opensearch.api.search.error.SearchExceptionHandler;
import com.kimai07.opensearch.api.search.error.SearchUnavailableException;
import com.kimai07.opensearch.api.search.model.ArticleHit;
import com.kimai07.opensearch.api.search.model.ArticleSearchResult;
import com.kimai07.opensearch.api.search.service.ArticleSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArticleSearchController.class)
@Import(SearchExceptionHandler.class)
class ArticleSearchControllerTest {

    private static final String PATH = "/api/v1/articles/search";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleSearchService service;

    @Test
    @DisplayName("ヒットありの場合、レスポンスが200で検索結果のJSONであること")
    void testSearchReturnsResults() throws Exception {
        ArticleHit hit = new ArticleHit("1", 1.5, "Title", "Content", "technology",
                Map.of("title", List.of("<em>Title</em>")));
        when(service.search("OpenSearch", 5)).thenReturn(new ArticleSearchResult(1, List.of(hit)));

        mockMvc.perform(get(PATH).param("q", "OpenSearch").param("size", "5")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.hits[0].id").value("1"))
                .andExpect(jsonPath("$.hits[0].score").value(1.5)).andExpect(jsonPath("$.hits[0].title").value("Title"))
                .andExpect(jsonPath("$.hits[0].content").value("Content"))
                .andExpect(jsonPath("$.hits[0].category").value("technology"))
                .andExpect(jsonPath("$.hits[0].highlights.title[0]").value("<em>Title</em>"));
    }

    @Test
    @DisplayName("sizeを省略した場合、検索のサイズが10であること")
    void testSearchUsesDefaultSize() throws Exception {
        when(service.search("OpenSearch", 10)).thenReturn(new ArticleSearchResult(0, List.of()));

        mockMvc.perform(get(PATH).param("q", "OpenSearch")).andExpect(status().isOk());

        verify(service).search("OpenSearch", 10);
    }

    @Test
    @DisplayName("qを省略した場合、レスポンスが400であること")
    void testSearchRejectsMissingQuery() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("qが空白の場合、レスポンスが400であること")
    void testSearchRejectsBlankQuery() throws Exception {
        mockMvc.perform(get(PATH).param("q", "  ")).andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("sizeが0の場合、レスポンスが400であること")
    void testSearchRejectsTooSmallSize() throws Exception {
        mockMvc.perform(get(PATH).param("q", "OpenSearch").param("size", "0")).andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("sizeが101の場合、レスポンスが400であること")
    void testSearchRejectsTooLargeSize() throws Exception {
        mockMvc.perform(get(PATH).param("q", "OpenSearch").param("size", "101")).andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("sizeが数値でない場合、レスポンスが400であること")
    void testSearchRejectsNonNumericSize() throws Exception {
        mockMvc.perform(get(PATH).param("q", "OpenSearch").param("size", "abc")).andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("検索が利用できない場合、レスポンスが503で内部情報を含まない内容であること")
    void testSearchReturnsServiceUnavailable() throws Exception {
        when(service.search(anyString(), anyInt()))
                .thenThrow(new SearchUnavailableException("internal-host:9200 refused", new RuntimeException()));

        mockMvc.perform(get(PATH).param("q", "OpenSearch")).andExpect(status().isServiceUnavailable())
                .andExpect(content().string(not(containsString("internal-host"))));
    }
}
