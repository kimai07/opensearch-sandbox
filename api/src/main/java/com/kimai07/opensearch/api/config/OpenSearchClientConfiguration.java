package com.kimai07.opensearch.api.config;

import com.kimai07.opensearch.client.OpenSearchClientFactory;
import com.kimai07.opensearch.search.FullTextSearchService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * coreモジュールのクライアントと検索サービスをBeanとして登録する。
 */
@Configuration(proxyBeanMethods = false)
public class OpenSearchClientConfiguration {

    @Bean(destroyMethod = "close")
    public OpenSearchClientFactory openSearchClientFactory(OpenSearchProperties properties) {
        return new OpenSearchClientFactory(properties.toConfig());
    }

    @Bean
    public FullTextSearchService fullTextSearchService(OpenSearchClientFactory clientFactory) {
        return new FullTextSearchService(clientFactory);
    }
}
