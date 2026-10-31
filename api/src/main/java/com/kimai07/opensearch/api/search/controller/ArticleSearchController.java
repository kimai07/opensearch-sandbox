package com.kimai07.opensearch.api.search.controller;

import com.kimai07.opensearch.api.search.dto.ArticleSearchResponse;
import com.kimai07.opensearch.api.search.service.ArticleSearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 記事検索のAPI。
 */
@RestController
@RequestMapping("/api/v1/articles")
public class ArticleSearchController {

    private final ArticleSearchService service;

    public ArticleSearchController(ArticleSearchService service) {
        this.service = service;
    }

    /**
     * 記事を検索する。
     *
     * @param q
     *            検索文字列
     * @param size
     *            取得する最大件数（1〜100）
     *
     * @return 検索結果
     */
    @GetMapping("/search")
    public ArticleSearchResponse search(@RequestParam @NotBlank String q,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ArticleSearchResponse.from(service.search(q, size));
    }
}
