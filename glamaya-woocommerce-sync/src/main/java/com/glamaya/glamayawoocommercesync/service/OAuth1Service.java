package com.glamaya.glamayawoocommercesync.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

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

@Service
@RequiredArgsConstructor
public class OAuth1Service {

    @Value("${external.woocommerce.api.url}")
    private final String baseUrl;
    @Value("${external.woocommerce.api.wc-client-key}")
    private final String clientKey;
    @Value("${external.woocommerce.api.wc-client-secret}")
    private final String clientSecret;

    public String generateOAuth1Header(String url, Map<String, String> queryParams) {
        String method = HttpMethod.GET.name();
        url = baseUrl.concat(url);
        long timestamp = System.currentTimeMillis() / 1000;
        String nonce = UUID.randomUUID().toString().replace("-", "");

        // Add OAuth parameters
        Map<String, String> oauthParams = new HashMap<>();
        oauthParams.put("oauth_consumer_key", clientKey);
        oauthParams.put("oauth_nonce", nonce);
        oauthParams.put("oauth_signature_method", "HMAC-SHA1");
        oauthParams.put("oauth_timestamp", String.valueOf(timestamp));
        oauthParams.put("oauth_version", "1.0");

        // Merge OAuth parameters and query parameters
        Map<String, String> allParams = new HashMap<>(oauthParams);
        allParams.putAll(queryParams);

        // Generate the base string including query parameters
        String baseString = generateBaseString(url, method, allParams);
        String signature = generateSignature(baseString, clientSecret);

        // Add the signature to the OAuth parameters
        oauthParams.put("oauth_signature", signature);

        // Build the OAuth header
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
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secretKeySpec = new SecretKeySpec((consumerSecret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1");
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