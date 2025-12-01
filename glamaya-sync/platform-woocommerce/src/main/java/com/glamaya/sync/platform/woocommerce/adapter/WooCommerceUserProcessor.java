package com.glamaya.sync.platform.woocommerce.adapter;

import com.glamaya.datacontracts.ecommerce.Contact;
import com.glamaya.datacontracts.woocommerce.User;
import com.glamaya.sync.core.domain.model.ProcessorType;
import com.glamaya.sync.core.domain.port.out.DataMapper;
import com.glamaya.sync.core.domain.port.out.DataProvider;
import com.glamaya.sync.core.domain.port.out.ProcessorConfiguration;
import com.glamaya.sync.core.domain.port.out.SyncProcessor;
import com.glamaya.sync.platform.woocommerce.adapter.client.WooCommerceUserDataProvider;
import com.glamaya.sync.platform.woocommerce.adapter.mapper.WooCommerceUserDataMapper;
import com.glamaya.sync.platform.woocommerce.config.APIConfig;
import com.glamaya.sync.platform.woocommerce.config.WooCommerceEndpointConfiguration;
import org.springframework.stereotype.Component;

/**
 * Concrete implementation of SyncProcessor for WooCommerce Users.
 * This class groups the DataProvider, DataMapper, and configuration for WooCommerce Users,
 * making them available to the SyncOrchestrationService in a type-safe manner.
 */
@Component
public class WooCommerceUserProcessor implements SyncProcessor<User, Contact, APIConfig> {

    private final WooCommerceUserDataProvider dataProvider;
    private final WooCommerceUserDataMapper dataMapper;
    private final ProcessorConfiguration<APIConfig> configuration;

    public WooCommerceUserProcessor(
            WooCommerceUserDataProvider dataProvider,
            WooCommerceUserDataMapper dataMapper,
            WooCommerceEndpointConfiguration configProvider) {
        this.dataProvider = dataProvider;
        this.dataMapper = dataMapper;
        this.configuration = configProvider.getConfiguration(ProcessorType.WOOCOMMERCE_USER);
    }

    @Override
    public DataProvider<User> getDataProvider() {
        return dataProvider;
    }

    @Override
    public DataMapper<User, Contact> getDataMapper() {
        return dataMapper;
    }

    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.WOOCOMMERCE_USER;
    }

    @Override
    public ProcessorConfiguration<APIConfig> getConfiguration() {
        return configuration;
    }
}
