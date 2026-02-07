package com.kimai07.opensearch.client;

import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import java.io.IOException;

/**
 * 検索操作のインターフェース。
 * テスト時にモック可能にするために導入。
 */
public interface SearchClient {

    /**
     * 検索を実行する。
     *
     * @param request 検索リクエスト
     * @param clazz   結果をマッピングするクラス
     * @param <T>     ドキュメントの型
     * @return 検索レスポンス
     * @throws IOException 通信エラーが発生した場合
     */
    <T> SearchResponse<T> search(SearchRequest request, Class<T> clazz) throws IOException;
}