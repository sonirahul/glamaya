package com.glamaya.glamayawoocommercesync.config.webclient;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Configuration class for setting up and configuring {@link WebClient} instances.
 * This class provides specialized {@code WebClient} beans for different external services,
 * such as WooCommerce API and n8n webhooks, with specific base URLs and timeouts.
 */
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

    /**
     * Configures and provides a {@link WebClient} bean for interacting with the WooCommerce API.
     * This client is qualified as "woocommerceWebClient" and also tagged with {@link WebhookTarget}
     * for potential reuse in webhook scenarios if applicable.
     *
     * @return A configured {@link WebClient} instance for WooCommerce.
     */
    @Bean(name = "woocommerceWebClient")
    @WebhookTarget // Tagged for potential reuse in webhook scenarios
    public WebClient woocommerceWebClient() {
        return WebClient.builder()
                .baseUrl(woocommerceApiUrl)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .responseTimeout(Duration.ofMillis(responseTimeoutInMillis))))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySize))
                // .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) // Commented out: specific content type might be set per request
                .build();
    }

    /**
     * Configures and provides a {@link WebClient} bean for sending notifications to n8n webhooks.
     * This client is qualified as "n8nWebhookWebClient" and also tagged with {@link WebhookTarget}.
     *
     * @return A configured {@link WebClient} instance for n8n webhooks.
     */
    @Bean(name = "n8nWebhookWebClient")
    @WebhookTarget
    public WebClient n8nWebhookWebClient() {
        return WebClient.builder()
                .baseUrl(n8nApiUrl)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .responseTimeout(Duration.ofMillis(responseTimeoutInMillis))))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySize))
                // .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) // Commented out: specific content type might be set per request
                .build();
    }
}
