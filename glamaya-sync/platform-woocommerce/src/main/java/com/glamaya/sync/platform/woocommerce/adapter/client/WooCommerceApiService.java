package com.glamaya.sync.platform.woocommerce.adapter.client;

import com.glamaya.sync.core.domain.model.ProcessorStatus;
import com.glamaya.sync.platform.woocommerce.adapter.client.strategy.WooCommerceEntityStrategy;
import com.glamaya.sync.platform.woocommerce.config.APIConfig;
import com.glamaya.sync.platform.woocommerce.port.out.OAuthSignerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A generic service for interacting with the WooCommerce API.
 * It uses a strategy pattern to fetch different types of entities (Orders, Products, etc.).
 *
 * @param <E> The type of the entity to fetch.
 */
@Service
public class WooCommerceApiService<E> {

    private static final Logger log = LoggerFactory.getLogger(WooCommerceApiService.class);

    private final WebClient webClient;
    private final OAuthSignerPort oAuthSigner;

    public WooCommerceApiService(WebClient webClient, OAuthSignerPort oAuthSigner) {
        this.webClient = webClient;
        this.oAuthSigner = oAuthSigner;
    }

    public List<E> fetch(WooCommerceEntityStrategy<E> strategy, ProcessorStatus status, APIConfig config) {
        String relativeUrl = config.getQueryUrl();
        int page = status.getNextPage();

        try {
            Map<String, String> queryParams = buildQueryParams(status, config);
            List<E> entities = executeApiRequest(strategy, relativeUrl, queryParams);
            updateStatus(strategy, status, entities, config);
            return entities;
        } catch (Exception e) {
            log.error("Failed to fetch {} from WooCommerce API on page {}: {}",
                    relativeUrl, page, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch data from " + relativeUrl + " on page " + page, e);
        }
    }

    private Map<String, String> buildQueryParams(ProcessorStatus status, APIConfig config) {
        Map<String, String> queryParams = new HashMap<>(Map.of(
                "page", String.valueOf(status.getNextPage()),
                "per_page", String.valueOf(config.getPageSize()),
                "orderby", "modified",
                "order", "asc",
                "status", "any"
        ));

        if (status.isUseLastDateModifiedInQuery() && status.getLastDateModified() != null) {
            queryParams.put("after", DateTimeFormatter.ISO_INSTANT.format(status.getLastDateModified()));
        }
        return queryParams;
    }

    private List<E> executeApiRequest(WooCommerceEntityStrategy<E> strategy, String relativeUrl, Map<String, String> queryParams) {
        String oauthHeader = oAuthSigner.generateOAuth1Header(relativeUrl, queryParams);
        log.info("Making GET request to WooCommerce API: {} with query params: {}", relativeUrl, queryParams);

        return webClient.get()
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
                .bodyToMono(strategy.getListTypeReference())
                .block();
    }

    private void updateStatus(WooCommerceEntityStrategy<E> strategy, ProcessorStatus status, List<E> entities, APIConfig config) {
        if (entities == null || entities.isEmpty()) {
            log.info("Received null/empty response for {} on page {}. Assuming no new data.",
                    config.getQueryUrl(), status.getNextPage());
            status.setMoreDataAvailable(false);
            status.setNextPage(config.getInitPage());
            status.setUseLastDateModifiedInQuery(true);
            return;
        }

        log.info("Successfully retrieved {} entities from page {}.", entities.size(), status.getNextPage());
        status.setTotalItemsSynced(status.getTotalItemsSynced() + entities.size());
        E lastEntity = entities.get(entities.size() - 1);
        status.setLastDateModified(strategy.getLastModifiedExtractor().apply(lastEntity));

        if (entities.size() < config.getPageSize()) {
            status.setMoreDataAvailable(false);
            status.setNextPage(config.getInitPage());
            status.setUseLastDateModifiedInQuery(true);
        } else {
            status.setNextPage(status.getNextPage() + 1);
            status.setUseLastDateModifiedInQuery(false);
        }
    }
}
