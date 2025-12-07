package com.glamaya.sync.platform.woocommerce.adapter;

import com.glamaya.datacontracts.ecommerce.Contact;
import com.glamaya.sync.core.domain.model.EcomModel;
import com.glamaya.sync.core.domain.model.ProcessorType;
import com.glamaya.sync.platform.woocommerce.adapter.client.WooCommerceOrderDataProvider;
import com.glamaya.sync.platform.woocommerce.adapter.mapper.WooCommerceOrderToContactDataMapper;
import com.glamaya.sync.platform.woocommerce.config.WooCommerceEndpointConfiguration;
import org.springframework.stereotype.Component;

/**
 * Concrete implementation of SyncProcessor for WooCommerce OrderToContact.
 * This class groups the DataProvider, DataMapper, and configuration for WooCommerce OrderToContact,
 * making them available to the SyncOrchestrationService in a type-safe manner.
 */
@Component
public class WooCommerceOrderToContactProcessor extends AbstractWooCommerceProcessor<com.glamaya.datacontracts.woocommerce.Order, EcomModel<Contact>> {

    public WooCommerceOrderToContactProcessor(
            WooCommerceOrderDataProvider dataProvider, // use order data provider only, no need to have order to contact data provider
            WooCommerceOrderToContactDataMapper dataMapper,
            WooCommerceEndpointConfiguration configProvider) {
        super(
                dataProvider,
                dataMapper,
                ProcessorType.WOOCOMMERCE_ORDER_TO_CONTACT,
                configProvider.getConfiguration(ProcessorType.WOOCOMMERCE_ORDER_TO_CONTACT)
        );
    }
}
