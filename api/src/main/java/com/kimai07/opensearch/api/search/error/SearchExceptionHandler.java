package com.kimai07.opensearch.api.search.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 検索機能の例外をレスポンスに変換する。
 */
@RestControllerAdvice
public class SearchExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SearchExceptionHandler.class);

    /**
     * 検索エンジンの障害を503に変換する。例外の詳細はログにだけ出力する。
     *
     * @param exception
     *            発生した例外
     *
     * @return エラーレスポンス
     */
    @ExceptionHandler(SearchUnavailableException.class)
    public ProblemDetail handleSearchUnavailable(SearchUnavailableException exception) {
        logger.error("Search is unavailable", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Search is temporarily unavailable");
    }
}
