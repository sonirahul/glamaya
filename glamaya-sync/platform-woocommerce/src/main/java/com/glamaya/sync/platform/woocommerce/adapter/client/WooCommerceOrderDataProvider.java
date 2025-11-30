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
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Implementation of DataProvider for fetching WooCommerce Orders.
 * This adapter uses Spring WebClient and OAuth 1.0a to interact with the WooCommerce API.
 */
@Component
public class WooCommerceOrderDataProvider implements DataProvider<Order> {

    private static final Logger log = LoggerFactory.getLogger(WooCommerceOrderDataProvider.class);

    public static final Function<String, Instant> STRING_DATE_TO_INSTANT_FUNCTION = date ->
            Optional.ofNullable(date).filter(StringUtils::hasText)
                    .map(d -> LocalDateTime.parse(d, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC))
                    .orElse(null);

    private final WebClient webClient;
    private final OAuthSignerPort oAuthSigner;

    public WooCommerceOrderDataProvider(WebClient webClient, OAuthSignerPort oAuthSigner) {
        this.webClient = webClient;
        this.oAuthSigner = oAuthSigner;
    }

    @Override
    public List<Order> fetchData(SyncContext<?> context) {
        var config = (APIConfig) context.configuration().get();
        var status = context.status();

        String relativeUrl = config.getQueryUrl();
        Integer pageSize = config.getPageSize();
        int page = status.getNextPage();

        Map<String, String> queryParams = new HashMap<>(Map.of(
                "page", String.valueOf(page),
                "per_page", String.valueOf(pageSize),
                "orderby", "modified",
                "order", "asc",
                "status", "any"
        ));

        if (status.isUseLastDateModifiedInQuery() && status.getLastDateModified() != null) {
            queryParams.put("after", DateTimeFormatter.ISO_INSTANT.format(status.getLastDateModified()));
        }

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
                    .onStatus(httpStatusCode -> httpStatusCode.is4xxClientError() || httpStatusCode.is5xxServerError(),
                            resp -> resp.bodyToMono(String.class)
                                    .defaultIfEmpty("<empty body>")
                                    .flatMap(body -> {
                                        log.error("WooCommerce API Error: {} - {}", resp.statusCode(), body);
                                        return Mono.error(new RuntimeException("Remote API Error: " + resp.statusCode() + " - " + body));
                                    }))
                    .bodyToMono(orderListType)
                    .block();

            if (orders == null || orders.isEmpty()) {
                log.info("Received null/empty response for orders on page {}. Assuming no new orders.", page);
                status.setMoreDataAvailable(false);
                status.setNextPage(config.getInitPage());
                status.setUseLastDateModifiedInQuery(true);
                return List.of();
            }

            log.info("Successfully retrieved {} orders from page {}.", orders.size(), page);
            status.setTotalItemsSynced(status.getTotalItemsSynced() + orders.size());
            status.setLastDateModified(STRING_DATE_TO_INSTANT_FUNCTION.apply(orders.get(orders.size() - 1).getDateModifiedGmt()));

            if (orders.size() < pageSize) {
                status.setMoreDataAvailable(false);
                status.setNextPage(config.getInitPage());
                status.setUseLastDateModifiedInQuery(true);
            } else {
                status.setNextPage(status.getNextPage() + 1);
                status.setUseLastDateModifiedInQuery(false);
            }
            return orders;
        } catch (Exception e) {
            log.error("Failed to fetch orders from WooCommerce API on page {}: {}", page, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch data from WooCommerce on page " + page, e);
        }
    }
}
