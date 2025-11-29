package com.glamaya.sync.platform.woocommerce.adapter.client;

import com.glamaya.datacontracts.woocommerce.Order;
import com.glamaya.sync.core.domain.model.SyncContext;
import com.glamaya.sync.core.domain.port.out.DataProvider;
import com.glamaya.sync.platform.woocommerce.config.APIConfig;
import com.glamaya.sync.platform.woocommerce.port.out.OAuthSignerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of DataProvider for fetching WooCommerce Orders.
 * This adapter uses Spring WebClient and OAuth 1.0a to interact with the WooCommerce API.
 */
@Component
public class WooCommerceOrderDataProvider implements DataProvider<Order> {

    private static final Logger log = LoggerFactory.getLogger(WooCommerceOrderDataProvider.class);
    private final WebClient webClient;
    private final OAuthSignerPort oAuthSigner;

    public WooCommerceOrderDataProvider(WebClient webClient, OAuthSignerPort oAuthSigner) {
        this.webClient = webClient;
        this.oAuthSigner = oAuthSigner;
    }

    @Override
    public List<Order> fetchData(SyncContext context) {
        APIConfig apiConfig = (APIConfig) context.configuration().get();

        String relativeUrl = apiConfig.getQueryUrl();
        if (relativeUrl == null || relativeUrl.isBlank()) {
            throw new IllegalStateException("Missing required WooCommerce orders query URL in APIConfig (glamaya.sync.woocommerce.api.orders-config.query-url)");
        }

        int page = context.status().getCurrentPage();

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("page", String.valueOf(page));
        queryParams.put("per_page", String.valueOf(apiConfig.getPageSize()));
        queryParams.put("orderby", "modified");
        queryParams.put("order", "asc");
        queryParams.put("status", "any");

        Optional.ofNullable(context.status().getLastSuccessfulRun())
                .ifPresent(lastRun -> {
                    String iso8601Time = DateTimeFormatter.ISO_INSTANT.format(lastRun);
                    queryParams.put("after", iso8601Time);
                });

        String oauthHeader = oAuthSigner.generateOAuth1Header(relativeUrl, queryParams);
        log.info("Making GET request to WooCommerce API: {} with query params: {}", relativeUrl, queryParams);

        try {
            ParameterizedTypeReference<List<Order>> orderListType = new ParameterizedTypeReference<>() {};

            List<Order> orders = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(relativeUrl);
                        queryParams.forEach(uriBuilder::queryParam);
                        return uriBuilder.build();
                    })
                    .header(HttpHeaders.AUTHORIZATION, oauthHeader)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            resp -> resp.bodyToMono(String.class)
                                    .defaultIfEmpty("<empty body>")
                                    .flatMap(body -> {
                                        log.error("WooCommerce API Error: {} - {}", resp.statusCode(), body);
                                        return Mono.error(new RuntimeException("Remote API Error: " + resp.statusCode() + " - " + body));
                                    }))
                    .bodyToMono(orderListType)
                    .block();

            if (orders != null) {
                log.info("Successfully retrieved {} orders from page {}.", orders.size(), page);
            } else {
                log.info("Received null response for orders on page {}. Assuming no new orders.", page);
                return List.of();
            }

            return orders;

        } catch (Exception e) {
            log.error("Failed to fetch orders from WooCommerce API on page {}: {}", page, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch data from WooCommerce on page " + page, e);
        }
    }
}
