package com.glamaya.sync.platform.woocommerce.adapter;

import com.glamaya.datacontracts.ecommerce.Order;
import com.glamaya.sync.core.domain.model.ProcessorType;
import com.glamaya.sync.platform.woocommerce.adapter.client.WooCommerceOrderDataProvider;
import com.glamaya.sync.platform.woocommerce.adapter.mapper.WooCommerceOrderDataMapper;
import com.glamaya.sync.platform.woocommerce.config.WooCommerceEndpointConfiguration;
import org.springframework.stereotype.Component;

/**
 * Concrete implementation of SyncProcessor for WooCommerce Orders.
 * This class groups the DataProvider, DataMapper, and configuration for WooCommerce Orders,
 * making them available to the SyncOrchestrationService in a type-safe manner.
 */
@Component
public class WooCommerceOrderProcessor extends AbstractWooCommerceProcessor<com.glamaya.datacontracts.woocommerce.Order, Order> {

    public WooCommerceOrderProcessor(
            WooCommerceOrderDataProvider dataProvider,
            WooCommerceOrderDataMapper dataMapper,
            WooCommerceEndpointConfiguration configProvider) {
        super(
                dataProvider,
                dataMapper,
                ProcessorType.WOOCOMMERCE_ORDER,
                configProvider.getConfiguration(ProcessorType.WOOCOMMERCE_ORDER)
        );
    }
}
