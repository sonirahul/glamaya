package com.glamaya.sync.platform.woocommerce.adapter.client;

import com.glamaya.datacontracts.woocommerce.Order;
import com.glamaya.sync.core.domain.model.SyncContext;
import com.glamaya.sync.core.domain.port.out.DataProvider;
import com.glamaya.sync.platform.woocommerce.adapter.client.strategy.OrderStrategy;
import com.glamaya.sync.platform.woocommerce.config.APIConfig;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementation of DataProvider for fetching WooCommerce Orders.
 * This class acts as a thin wrapper, delegating the actual API interaction
 * to the generic WooCommerceApiService, configured with an OrderStrategy.
 */
@Component
public class WooCommerceOrderDataProvider implements DataProvider<Order> {

    private final WooCommerceApiService<Order> apiService;
    private final OrderStrategy orderStrategy;

    public WooCommerceOrderDataProvider(WooCommerceApiService<Order> apiService, OrderStrategy orderStrategy) {
        this.apiService = apiService;
        this.orderStrategy = orderStrategy;
    }

    @Override
    public List<Order> fetchData(SyncContext<?> context) {
        // The context configuration is expected to be an APIConfig instance.
        var config = (APIConfig) context.configuration().get();
        var status = context.status();

        // Delegate the entire fetch operation to the generic service.
        return apiService.fetch(orderStrategy, status, config);
    }
}
