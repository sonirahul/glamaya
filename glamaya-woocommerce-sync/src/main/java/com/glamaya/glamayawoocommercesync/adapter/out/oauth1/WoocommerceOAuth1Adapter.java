package com.glamaya.glamayawoocommercesync.adapter.out.oauth1;

import com.glamaya.glamayawoocommercesync.port.out.OAuthSignerPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component; // Changed to Component

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

@Component // Changed to Component
public class WoocommerceOAuth1Adapter implements OAuthSignerPort { // Implements the new interface

    @Value("${external.woocommerce.api.url}")
    private String baseUrl;
    @Value("${external.woocommerce.api.wc-client-key}")
    private String clientKey;
    @Value("${external.woocommerce.api.wc-client-secret}")
    private String clientSecret;

    private volatile SecretKeySpec cachedKeySpec;
    private final ThreadLocal<Mac> macThreadLocal = ThreadLocal.withInitial(() -> {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            ensureKeySpec();
            mac.init(cachedKeySpec);
            return mac;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to init HmacSHA1 Mac", e);
        }
    });

    private void ensureKeySpec() {
        if (cachedKeySpec == null && clientSecret != null) {
            cachedKeySpec = new SecretKeySpec((clientSecret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        }
    }

    @Override // Implements the interface method
    public String generateOAuth1Header(String url, Map<String, String> queryParams) {
        ensureKeySpec();
        String method = HttpMethod.GET.name();
        url = baseUrl.concat(url);
        long timestamp = System.currentTimeMillis() / 1000;
        String nonce = UUID.randomUUID().toString().replace("-", "");

        Map<String, String> oauthParams = new HashMap<>();
        oauthParams.put("oauth_consumer_key", clientKey);
        oauthParams.put("oauth_nonce", nonce);
        oauthParams.put("oauth_signature_method", "HMAC-SHA1");
        oauthParams.put("oauth_timestamp", String.valueOf(timestamp));
        oauthParams.put("oauth_version", "1.0");

        // Robust filtering: treat any value via String.valueOf, then check blank
        Map<String, String> filteredQueryParams = (queryParams == null ? Map.<String, String>of() :
                queryParams.entrySet().stream()
                        .filter(e -> e.getValue() != null && !String.valueOf(e.getValue()).isBlank())
                        .map(e -> Map.entry(e.getKey(), String.valueOf(e.getValue())))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        Map<String, String> allParams = new HashMap<>(oauthParams);
        allParams.putAll(filteredQueryParams);

        String baseString = generateBaseString(url, method, allParams);
        String signature = generateSignature(baseString, clientSecret);
        oauthParams.put("oauth_signature", signature);

        return "OAuth " + oauthParams.entrySet().stream()
                .map(entry -> String.format("%s=\"%s\"", entry.getKey(), urlEncode(entry.getValue())))
                .collect(Collectors.joining(", "));
    }

    private String generateBaseString(String url, String method, Map<String, String> oauthParams) {
        String baseString = method.toUpperCase() + "&" + urlEncode(url) + "&";
        String encodedParams = oauthParams.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
        return baseString + urlEncode(encodedParams);
    }

    private String generateSignature(String baseString, String consumerSecret) {
        try {
            ensureKeySpec();
            Mac mac = macThreadLocal.get();
            if (consumerSecret != null && !consumerSecret.equals(clientSecret)) {
                mac.init(new SecretKeySpec((consumerSecret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            } else if (cachedKeySpec != null) {
                mac.init(cachedKeySpec);
            }
            mac.reset();
            byte[] signatureBytes = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Failed to generate OAuth signature", e);
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
