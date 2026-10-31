package com.kimai07.opensearch.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 検索APIのエントリーポイント。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class OpenSearchApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenSearchApiApplication.class, args);
    }
}
