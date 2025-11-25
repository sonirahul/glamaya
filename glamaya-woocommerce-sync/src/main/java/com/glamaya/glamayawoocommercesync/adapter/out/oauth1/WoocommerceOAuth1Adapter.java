package com.glamaya.glamayawoocommercesync.adapter.out.oauth1;

import com.glamaya.glamayawoocommercesync.port.out.OAuthSignerPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * An outbound adapter that implements the {@link OAuthSignerPort} for WooCommerce's OAuth 1.0a authentication.
 * This class handles the generation of OAuth 1.0a signatures and authorization headers,
 * abstracting the cryptographic details from the application core.
 */
@Component
public class WoocommerceOAuth1Adapter implements OAuthSignerPort {

    @Value("${external.woocommerce.api.url}")
    private String baseUrl;
    @Value("${external.woocommerce.api.wc-client-key}")
    private String clientKey;
    @Value("${external.woocommerce.api.wc-client-secret}")
    private String clientSecret;

    private volatile SecretKeySpec cachedKeySpec;

    // ThreadLocal for Mac instances to ensure thread safety and performance
    private final ThreadLocal<Mac> macThreadLocal = ThreadLocal.withInitial(() -> {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            ensureKeySpec(); // Ensure key is initialized before first use
            mac.init(cachedKeySpec);
            return mac;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize HmacSHA1 Mac instance", e);
        }
    });

    /**
     * Ensures that the {@code SecretKeySpec} for HMAC-SHA1 is initialized.
     * This method is synchronized to prevent race conditions during initialization.
     */
    private synchronized void ensureKeySpec() {
        if (cachedKeySpec == null && clientSecret != null) {
            cachedKeySpec = new SecretKeySpec((clientSecret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        }
    }

    /**
     * Generates an OAuth 1.0a authorization header for a given URL and query parameters.
     *
     * @param url         The relative URL of the WooCommerce API endpoint.
     * @param queryParams A map of query parameters to be included in the signature base string.
     * @return The complete OAuth 1.0a authorization header string.
     */
    @Override
    public String generateOAuth1Header(String url, Map<String, String> queryParams) {
        ensureKeySpec(); // Ensure key is available

        String method = HttpMethod.GET.name(); // Assuming GET for fetching data
        String fullUrl = baseUrl.concat(url);
        long timestamp = System.currentTimeMillis() / 1000;
        String nonce = UUID.randomUUID().toString().replace("-", ""); // Generate a random nonce

        // Build OAuth parameters
        Map<String, String> oauthParams = new HashMap<>();
        oauthParams.put("oauth_consumer_key", clientKey);
        oauthParams.put("oauth_nonce", nonce);
        oauthParams.put("oauth_signature_method", "HMAC-SHA1");
        oauthParams.put("oauth_timestamp", String.valueOf(timestamp));
        oauthParams.put("oauth_version", "1.0");

        // Filter out null or blank query parameters
        Map<String, String> filteredQueryParams = (queryParams == null ? Map.<String, String>of() :
                queryParams.entrySet().stream()
                        .filter(entry -> entry.getValue() != null && !String.valueOf(entry.getValue()).isBlank())
                        .map(entry -> Map.entry(entry.getKey(), String.valueOf(entry.getValue())))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        // Combine all parameters for signature generation
        Map<String, String> allParams = new HashMap<>(oauthParams);
        allParams.putAll(filteredQueryParams);

        String baseString = generateBaseString(fullUrl, method, allParams);
        String signature = generateSignature(baseString, clientSecret);
        oauthParams.put("oauth_signature", signature);

        // Format into the final OAuth header string
        return "OAuth " + oauthParams.entrySet().stream()
                .map(entry -> String.format("%s=\"%s\"", entry.getKey(), urlEncode(entry.getValue())))
                .collect(Collectors.joining(", "));
    }

    /**
     * Generates the OAuth 1.0a signature base string.
     *
     * @param url         The full URL of the request.
     * @param method      The HTTP method (e.g., GET, POST).
     * @param oauthParams All OAuth and query parameters.
     * @return The encoded signature base string.
     */
    private String generateBaseString(String url, String method, Map<String, String> oauthParams) {
        // Base string format: HTTP_METHOD & url_encode(BASE_URL) & url_encode(NORMALIZED_PARAMETERS)
        String baseString = method.toUpperCase() + "&" + urlEncode(url) + "&";

        // Normalize parameters: sort by key, then URL-encode key and value, join with '&'
        String encodedParams = oauthParams.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));

        return baseString + urlEncode(encodedParams);
    }

    /**
     * Generates the HMAC-SHA1 signature for the given base string and consumer secret.
     *
     * @param baseString     The OAuth 1.0a signature base string.
     * @param consumerSecret The consumer secret.
     * @return The Base64 encoded signature.
     * @throws RuntimeException if signature generation fails.
     */
    private String generateSignature(String baseString, String consumerSecret) {
        try {
            ensureKeySpec(); // Ensure key is available
            Mac mac = macThreadLocal.get();

            // Re-initialize Mac if consumerSecret changes (unlikely in this context but good practice)
            if (consumerSecret != null && !consumerSecret.equals(clientSecret)) {
                mac.init(new SecretKeySpec((consumerSecret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            } else if (cachedKeySpec != null) {
                mac.init(cachedKeySpec);
            }

            mac.reset(); // Reset for reuse
            byte[] signatureBytes = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Failed to generate OAuth signature due to invalid key", e);
        }
    }

    /**
     * URL-encodes a string value according to OAuth 1.0a specifications.
     * Specifically, it replaces '+' with '%20'.
     *
     * @param value The string value to encode.
     * @return The URL-encoded string.
     */
    private String urlEncode(String value) {
        // URLEncoder encodes space as '+', but OAuth requires '%20'
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
