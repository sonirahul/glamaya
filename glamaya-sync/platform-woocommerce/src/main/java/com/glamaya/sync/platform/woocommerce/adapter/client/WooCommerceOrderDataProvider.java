package com.glamaya.sync.platform.woocommerce.adapter.client;

import com.glamaya.datacontracts.woocommerce.Order;
import com.glamaya.sync.core.domain.model.SyncContext;
import com.glamaya.sync.core.domain.port.out.DataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

/**
 * Implementation of DataProvider for fetching WooCommerce Orders.
 * This adapter uses Spring WebClient to interact with the WooCommerce API.
 */
@Component // Mark as a Spring component for dependency injection
public class WooCommerceOrderDataProvider implements DataProvider<Order> {

    private static final Logger log = LoggerFactory.getLogger(WooCommerceOrderDataProvider.class);
    private final WebClient webClient;

    // WebClient will be configured in WooCommerceModuleConfiguration
    public WooCommerceOrderDataProvider(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public List<Order> fetchData(SyncContext context) {
        log.info("Fetching WooCommerce orders with context: {}", context);

        // TODO: Implement actual API call to WooCommerce
        // This will involve using the webClient, potentially handling authentication,
        // and parsing the response into a List<Order>.
        // The SyncContext (especially cursor/currentPage from ProcessorStatus)
        // should be used to make incremental API calls.

        // Placeholder for actual API call
        // Example:
        // return webClient.get()
        //         .uri("/wp-json/wc/v3/orders?per_page=100&after={lastSuccessfulRun}", context.status().getLastSuccessfulRun())
        //         .retrieve()
        //         .bodyToFlux(Order.class)
        //         .collectList()
        //         .block(); // Blocking for simplicity, consider reactive approach in orchestrator if needed

        log.warn("WooCommerceOrderDataProvider.fetchData is currently a placeholder and returns an empty list.");
        return Collections.emptyList();
    }
}
