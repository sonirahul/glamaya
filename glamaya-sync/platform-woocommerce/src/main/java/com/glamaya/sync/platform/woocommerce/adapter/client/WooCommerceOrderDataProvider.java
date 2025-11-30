package com.glamaya.sync.platform.woocommerce.adapter.client;

import com.glamaya.datacontracts.woocommerce.Order;
import com.glamaya.sync.core.domain.model.SyncContext;
import com.glamaya.sync.core.domain.port.out.DataProvider;
import com.glamaya.sync.platform.woocommerce.adapter.client.descriptor.OrderDescriptor;
import com.glamaya.sync.platform.woocommerce.config.APIConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Implementation of DataProvider for fetching WooCommerce Orders.
 * This class orchestrates fetching a page and updating the status based on the result.
 */
@Slf4j
@Component
public class WooCommerceOrderDataProvider implements DataProvider<Order> {

    private final WooCommerceApiService<Order> apiService;
    private final OrderDescriptor orderDescriptor;

    public WooCommerceOrderDataProvider(WooCommerceApiService<Order> apiService, OrderDescriptor orderDescriptor) {
        this.apiService = apiService;
        this.orderDescriptor = orderDescriptor;
    }

    @Override
    public Flux<Order> fetchData(SyncContext<?> context) {
        var config = (APIConfig) context.configuration().get();
        var status = context.status();

        return apiService.fetchPage(orderDescriptor, status, config)
                .collectList()
                .doOnNext(pageItems -> WooCommerceApiService.updateStatusAfterPage(status, pageItems, config,
                        orderDescriptor.getLastModifiedExtractor()))
                .flatMapMany(Flux::fromIterable);
    }
}
