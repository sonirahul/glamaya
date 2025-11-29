package com.glamaya.sync.platform.woocommerce.adapter.oauth1;

import com.glamaya.sync.platform.woocommerce.port.out.OAuthSignerPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * An outbound adapter that implements the OAuthSignerPort for WooCommerce's OAuth 1.0a authentication.
 * This class handles the generation of OAuth 1.0a signatures and authorization headers.
 */
@Component
public class WooCommerceOAuth1Signer implements OAuthSignerPort {

    private final String baseUrl;
    private final String clientKey;
    private final String clientSecret;

    public WooCommerceOAuth1Signer(
            @Value("${glamaya.sync.woocommerce.api.baseUrl}") String baseUrl,
            @Value("${glamaya.sync.woocommerce.api.consumerKey}") String clientKey,
            @Value("${glamaya.sync.woocommerce.api.consumerSecret}") String clientSecret) {
        this.baseUrl = baseUrl;
        this.clientKey = clientKey;
        this.clientSecret = clientSecret;
    }

    @Override
    public String generateOAuth1Header(String url, Map<String, String> queryParams) {
        String method = HttpMethod.GET.name();
        String fullUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) + url : baseUrl + url;
        long timestamp = System.currentTimeMillis() / 1000;
        String nonce = UUID.randomUUID().toString();

        Map<String, String> oauthParams = new HashMap<>();
        oauthParams.put("oauth_consumer_key", clientKey);
        oauthParams.put("oauth_nonce", nonce);
        oauthParams.put("oauth_signature_method", "HMAC-SHA1");
        oauthParams.put("oauth_timestamp", String.valueOf(timestamp));
        oauthParams.put("oauth_version", "1.0");

        Map<String, String> allParams = new HashMap<>(oauthParams);
        if (queryParams != null) {
            allParams.putAll(queryParams);
        }

        String baseString = generateBaseString(fullUrl, method, allParams);
        String signature = generateSignature(baseString);
        oauthParams.put("oauth_signature", signature);

        return "OAuth " + oauthParams.entrySet().stream()
                .map(entry -> String.format("%s=\"%s\"", entry.getKey(), urlEncode(entry.getValue())))
                .collect(Collectors.joining(", "));
    }

    private String generateBaseString(String url, String method, Map<String, String> params) {
        String encodedParams = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
        return method.toUpperCase() + "&" + urlEncode(url) + "&" + urlEncode(encodedParams);
    }

    private String generateSignature(String baseString) {
        try {
            String key = clientSecret + "&";
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate OAuth signature", e);
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
