package com.kimai07.opensearch.api.search.error;

/**
 * 検索エンジンを利用できないことを表す例外。
 */
public class SearchUnavailableException extends RuntimeException {

    public SearchUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
