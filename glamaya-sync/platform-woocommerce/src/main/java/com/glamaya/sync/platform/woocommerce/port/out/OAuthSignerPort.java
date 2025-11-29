package com.glamaya.sync.platform.woocommerce.port.out;

import java.util.Map;

/**
 * An outbound port defining the contract for generating an OAuth 1.0a authorization header.
 */
public interface OAuthSignerPort {

    /**
     * Generates an OAuth 1.0a authorization header for a given URL and query parameters.
     *
     * @param url         The relative URL of the API endpoint (e.g., "/orders").
     * @param queryParams A map of query parameters to be included in the signature.
     * @return The complete OAuth 1.0a authorization header string (e.g., "OAuth oauth_consumer_key=...").
     */
    String generateOAuth1Header(String url, Map<String, String> queryParams);
}
