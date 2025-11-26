package com.glamaya.sync.platform.woocommerce.adapter;

import com.glamaya.datacontracts.ecommerce.Order;
import com.glamaya.sync.core.domain.model.ProcessorType;
import com.glamaya.sync.core.domain.port.out.DataMapper;
import com.glamaya.sync.core.domain.port.out.DataProvider;
import com.glamaya.sync.core.domain.port.out.SyncProcessor;
import com.glamaya.sync.platform.woocommerce.adapter.client.WooCommerceOrderDataProvider;
import com.glamaya.sync.platform.woocommerce.adapter.mapper.WooCommerceOrderDataMapper;
import org.springframework.stereotype.Component;

/**
 * Concrete implementation of SyncProcessor for WooCommerce Orders.
 * This class groups the DataProvider and DataMapper for WooCommerce Orders,
 * making them available to the SyncOrchestrationService in a type-safe manner.
 */
@Component
public class WooCommerceOrderProcessor implements SyncProcessor<com.glamaya.datacontracts.woocommerce.Order, Order> {

    private final WooCommerceOrderDataProvider dataProvider;
    private final WooCommerceOrderDataMapper dataMapper;

    public WooCommerceOrderProcessor(WooCommerceOrderDataProvider dataProvider, WooCommerceOrderDataMapper dataMapper) {
        this.dataProvider = dataProvider;
        this.dataMapper = dataMapper;
    }

    @Override
    public DataProvider<com.glamaya.datacontracts.woocommerce.Order> getDataProvider() {
        return dataProvider;
    }

    @Override
    public DataMapper<com.glamaya.datacontracts.woocommerce.Order, Order> getDataMapper() {
        return dataMapper;
    }

    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.WOOCOMMERCE_ORDER;
    }
}
