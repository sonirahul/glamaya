package com.glamaya.glamayawixsync.config.webclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Value("${external.wix.api.url}")
    private String wixApiUrl;

    @Value("${external.wix.api.key}")
    private String wixApiKey;

    @Value("${external.wix.api.account-id}")
    private String wixAccountId;

    @Value("${external.wix.api.site-id}")
    private String wixSiteId;

    @Value("${application.webclient.response-timeout-in-millis}")
    private long responseTimeoutInMillis;

    @Value("${application.webclient.max-in-memory-size-in-bytes}")
    private int maxInMemorySize;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(wixApiUrl)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .responseTimeout(Duration.ofMillis(responseTimeoutInMillis))))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySize))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, wixApiKey)
                .defaultHeader("wix-account-id", wixAccountId)
                .defaultHeader("wix-site-id", wixSiteId)
                .build();
    }
}
