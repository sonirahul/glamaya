package com.glamaya.glamayawoocommercesync.port.out;

import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Defines the outbound port for interacting with the WooCommerce API.
 * This interface abstracts the details of making HTTP requests to WooCommerce,
 * allowing the application core to remain decoupled from the underlying
 * communication technology (e.g., WebClient).
 */
public interface WooCommerceApiClientPort {
    /**
     * Fetches data from a specified WooCommerce API endpoint.
     *
     * @param url         The relative URL of the WooCommerce API endpoint.
     * @param queryParams A map of query parameters to include in the request.
     * @param oauthHeader The OAuth1 authorization header string.
     * @return A {@link Flux} emitting raw objects from the API response.
     */
    Flux<Object> fetch(String url, Map<String, String> queryParams, String oauthHeader);
}
