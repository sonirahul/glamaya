package com.glamaya.glamayawoocommercesync.config.webclient;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    @Value("${external.woocommerce.api.url}")
    private final String woocommerceApiUrl;

    @Value("${external.n8n.api.url}")
    private final String n8nApiUrl;

    @Value("${application.webclient.response-timeout-in-millis}")
    private final long responseTimeoutInMillis;

    @Value("${application.webclient.max-in-memory-size-in-bytes}")
    private final int maxInMemorySize;

    @Bean(name = "woocommerceWebClient")
    @WebhookTarget
    public WebClient woocommerceWebClient() {
        return WebClient.builder()
                .baseUrl(woocommerceApiUrl)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .responseTimeout(Duration.ofMillis(responseTimeoutInMillis))))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySize))
//                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean(name = "n8nWebhookWebClient")
    @WebhookTarget
    public WebClient n8nWebhookWebClient() {
        return WebClient.builder()
                .baseUrl(n8nApiUrl)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .responseTimeout(Duration.ofMillis(responseTimeoutInMillis))))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySize))
//                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
