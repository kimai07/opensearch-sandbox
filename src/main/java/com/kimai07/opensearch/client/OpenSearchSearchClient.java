package com.kimai07.opensearch.client;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import java.io.IOException;

/**
 * SearchClientのOpenSearch実装。
 */
public class OpenSearchSearchClient implements SearchClient {

    private final OpenSearchClient client;

    public OpenSearchSearchClient(OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public <T> SearchResponse<T> search(SearchRequest request, Class<T> clazz) throws IOException {
        return client.search(request, clazz);
    }
}