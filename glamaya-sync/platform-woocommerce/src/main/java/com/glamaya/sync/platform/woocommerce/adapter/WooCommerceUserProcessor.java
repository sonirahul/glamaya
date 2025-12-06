package com.glamaya.sync.platform.woocommerce.adapter;

import com.glamaya.datacontracts.ecommerce.Contact;
import com.glamaya.datacontracts.woocommerce.User;
import com.glamaya.sync.core.domain.model.ProcessorType;
import com.glamaya.sync.platform.woocommerce.adapter.client.WooCommerceUserDataProvider;
import com.glamaya.sync.platform.woocommerce.adapter.mapper.WooCommerceUserDataMapper;
import com.glamaya.sync.platform.woocommerce.config.WooCommerceEndpointConfiguration;
import org.springframework.stereotype.Component;

/**
 * Concrete implementation of SyncProcessor for WooCommerce Users.
 * This class groups the DataProvider, DataMapper, and configuration for WooCommerce Users,
 * making them available to the SyncOrchestrationService in a type-safe manner.
 */
@Component
public class WooCommerceUserProcessor extends AbstractWooCommerceProcessor<User, Contact> {

    public WooCommerceUserProcessor(
            WooCommerceUserDataProvider dataProvider,
            WooCommerceUserDataMapper dataMapper,
            WooCommerceEndpointConfiguration configProvider) {
        super(
                dataProvider,
                dataMapper,
                ProcessorType.WOOCOMMERCE_USER,
                configProvider.getConfiguration(ProcessorType.WOOCOMMERCE_USER)
        );
    }
}
