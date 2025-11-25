package com.glamaya.glamayawoocommercesync.adapter.out.woocommerce;

import com.glamaya.glamayawoocommercesync.port.out.WooCommerceApiClientPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * An outbound adapter that implements the {@link WooCommerceApiClientPort} using Spring's {@link WebClient}.
 * This class is responsible for making actual HTTP calls to the WooCommerce API,
 * handling request building, OAuth authorization, and basic error handling.
 */
@Component
public class WooCommerceApiWebClientAdapter implements WooCommerceApiClientPort {

    private final WebClient webClient;

    /**
     * Constructs a new {@code WooCommerceApiWebClientAdapter}.
     *
     * @param webClient The {@link WebClient} instance specifically configured for WooCommerce API calls.
     */
    public WooCommerceApiWebClientAdapter(@Qualifier("woocommerceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Fetches data from a specified WooCommerce API endpoint using {@link WebClient}.
     * It includes the OAuth1 authorization header and handles common HTTP error statuses.
     *
     * @param url         The relative URL of the WooCommerce API endpoint.
     * @param queryParams A map of query parameters to include in the request.
     * @param oauthHeader The OAuth1 authorization header string.
     * @return A {@link Flux} emitting raw objects from the API response.
     */
    @Override
    public Flux<Object> fetch(String url, Map<String, String> queryParams, String oauthHeader) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(url);
                    queryParams.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                })
                .header(HttpHeaders.AUTHORIZATION, oauthHeader == null ? "" : oauthHeader)
                .retrieve()
                .onStatus(status -> status.is5xxServerError() || status.value() == 429,
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("<empty>")
                                .flatMap(body -> Mono.error(new RuntimeException("Remote HTTP " + resp.statusCode().value() + ": " + body))))
                .bodyToFlux(Object.class);
    }
}
