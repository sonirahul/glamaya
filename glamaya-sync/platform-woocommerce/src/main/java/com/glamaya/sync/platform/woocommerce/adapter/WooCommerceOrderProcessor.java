package com.glamaya.sync.platform.woocommerce.adapter;

import com.glamaya.datacontracts.ecommerce.Order;
import com.glamaya.sync.core.domain.model.ProcessorType;
import com.glamaya.sync.core.domain.port.out.*;
import com.glamaya.sync.platform.woocommerce.adapter.client.WooCommerceOrderDataProvider;
import com.glamaya.sync.platform.woocommerce.adapter.mapper.WooCommerceOrderDataMapper;
import com.glamaya.sync.platform.woocommerce.config.APIConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Concrete implementation of SyncProcessor for WooCommerce Orders.
 * This class groups the DataProvider, DataMapper, and configuration for WooCommerce Orders,
 * making them available to the SyncOrchestrationService in a type-safe manner.
 */
@Component
public class WooCommerceOrderProcessor implements SyncProcessor<com.glamaya.datacontracts.woocommerce.Order, Order, APIConfig> {

    private final WooCommerceOrderDataProvider dataProvider;
    private final WooCommerceOrderDataMapper dataMapper;
    private final ProcessorConfiguration<APIConfig> configuration;

    public WooCommerceOrderProcessor(
            WooCommerceOrderDataProvider dataProvider,
            WooCommerceOrderDataMapper dataMapper,
            @Qualifier("wooOrdersApiConfig") APIConfig apiConfig) {
        this.dataProvider = dataProvider;
        this.dataMapper = dataMapper;
        this.configuration = new SimpleProcessorConfiguration<>(apiConfig);
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

    @Override
    public ProcessorConfiguration<APIConfig> getConfiguration() {
        return configuration;
    }
}
