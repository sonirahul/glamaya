package com.glamaya.sync.platform.woocommerce.adapter.mapper;

import com.glamaya.datacontracts.ecommerce.Order;
import com.glamaya.datacontracts.ecommerce.mapper.OrderMapperFactory;
import com.glamaya.sync.core.domain.port.out.DataMapper;
import org.springframework.stereotype.Component;

/**
 * Implementation of DataMapper for converting WooCommerce Order DTOs to Canonical Order domain models.
 */
@Component // Mark as a Spring component for dependency injection
public class WooCommerceOrderDataMapper implements DataMapper<com.glamaya.datacontracts.woocommerce.Order, Order> {

    private final OrderMapperFactory<com.glamaya.datacontracts.woocommerce.Order> orderMapperFactory;

    public WooCommerceOrderDataMapper(OrderMapperFactory<com.glamaya.datacontracts.woocommerce.Order> orderMapperFactory) {
        this.orderMapperFactory = orderMapperFactory;
    }

    @Override
    public Order mapToCanonical(com.glamaya.datacontracts.woocommerce.Order platformModel) {
        // Use the injected OrderMapperFactory to perform the conversion
        return orderMapperFactory.toGlamayaOrder(platformModel);
    }
}
