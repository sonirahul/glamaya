package com.glamaya.glamayawoocommercesync.port.out;

import java.util.Map;

/**
 * Defines the outbound port for generating OAuth 1.0a authorization headers.
 * This interface abstracts the details of the OAuth signing process,
 * allowing the application core to remain decoupled from the specific
 * OAuth implementation.
 */
public interface OAuthSignerPort {
    /**
     * Generates an OAuth 1.0a authorization header for a given URL and query parameters.
     *
     * @param url         The base URL for the request.
     * @param queryParams A map of query parameters to be included in the signature base string.
     * @return The complete OAuth 1.0a authorization header string.
     */
    String generateOAuth1Header(String url, Map<String, String> queryParams);
}
