package com.proyecto.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;


@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient bonitaRestClient(
            @Value("${bonita.base-url}") String bonitaBaseUrl
    ) {
        return RestClient.builder()
                .baseUrl(bonitaBaseUrl)
                .build();
    }
}
