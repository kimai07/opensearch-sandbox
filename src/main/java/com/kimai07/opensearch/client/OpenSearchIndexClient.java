package com.kimai07.opensearch.client;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch.indices.*;

import java.io.IOException;
import java.util.Map;

/**
 * IndexClientのOpenSearch実装。
 */
public class OpenSearchIndexClient implements IndexClient {

    private final OpenSearchClient client;

    public OpenSearchIndexClient(OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public boolean createIndex(String indexName, int numberOfShards, int numberOfReplicas,
                                Map<String, Property> properties, boolean enableKnn) throws IOException {
        CreateIndexRequest.Builder builder = new CreateIndexRequest.Builder()
                .index(indexName)
                .settings(s -> {
                    s.numberOfShards(numberOfShards).numberOfReplicas(numberOfReplicas);
                    if (enableKnn) {
                        s.knn(true);
                    }
                    return s;
                });

        if (properties != null && !properties.isEmpty()) {
            builder.mappings(m -> m.properties(properties));
        }

        CreateIndexResponse response = client.indices().create(builder.build());
        return response.acknowledged();
    }

    @Override
    public boolean deleteIndex(String indexName) throws IOException {
        DeleteIndexResponse response = client.indices().delete(d -> d.index(indexName));
        return response.acknowledged();
    }

    @Override
    public boolean indexExists(String indexName) throws IOException {
        return client.indices().exists(e -> e.index(indexName)).value();
    }

    @Override
    public boolean putMapping(String indexName, Map<String, Property> properties) throws IOException {
        PutMappingResponse response = client.indices().putMapping(p -> p
                .index(indexName)
                .properties(properties));
        return response.acknowledged();
    }

    @Override
    public boolean putIndexTemplate(String templateName, String indexPattern,
                                     int numberOfShards, int numberOfReplicas,
                                     Map<String, Property> properties) throws IOException {
        PutIndexTemplateResponse response = client.indices().putIndexTemplate(p -> p
                .name(templateName)
                .indexPatterns(indexPattern)
                .template(t -> t
                        .settings(s -> s
                                .numberOfShards(numberOfShards)
                                .numberOfReplicas(numberOfReplicas))
                        .mappings(m -> m.properties(properties))));
        return response.acknowledged();
    }

    @Override
    public boolean deleteIndexTemplate(String templateName) throws IOException {
        DeleteIndexTemplateResponse response = client.indices().deleteIndexTemplate(d -> d.name(templateName));
        return response.acknowledged();
    }

    @Override
    public GetIndicesSettingsResponse getIndexSettings(String indexName) throws IOException {
        return client.indices().getSettings(g -> g.index(indexName));
    }

    @Override
    public GetMappingResponse getIndexMapping(String indexName) throws IOException {
        return client.indices().getMapping(g -> g.index(indexName));
    }

    @Override
    public void refreshIndex(String indexName) throws IOException {
        client.indices().refresh(r -> r.index(indexName));
    }
}