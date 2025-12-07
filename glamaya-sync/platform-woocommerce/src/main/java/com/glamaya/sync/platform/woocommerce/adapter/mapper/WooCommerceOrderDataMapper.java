package com.glamaya.sync.platform.woocommerce.adapter.mapper;

import com.glamaya.datacontracts.ecommerce.Order;
import com.glamaya.datacontracts.ecommerce.mapper.OrderMapperFactory;
import com.glamaya.sync.core.domain.model.EcomModel;
import com.glamaya.sync.core.domain.port.out.DataMapper;
import org.springframework.stereotype.Component;

/**
 * Implementation of DataMapper for converting WooCommerce Order DTOs to Canonical Order domain models.
 */
@Component // Mark as a Spring component for dependency injection
public class WooCommerceOrderDataMapper implements DataMapper<com.glamaya.datacontracts.woocommerce.Order, EcomModel<Order>> {

    private final OrderMapperFactory<com.glamaya.datacontracts.woocommerce.Order> orderMapperFactory;

    public WooCommerceOrderDataMapper(OrderMapperFactory<com.glamaya.datacontracts.woocommerce.Order> orderMapperFactory) {
        this.orderMapperFactory = orderMapperFactory;
    }

    @Override
    public EcomModel<Order> mapToCanonical(com.glamaya.datacontracts.woocommerce.Order platformModel) {
        // Use the injected OrderMapperFactory to perform the conversion

        var order = orderMapperFactory.toGlamayaOrder(platformModel);
        return new EcomModel<>(order.getId(), order);
    }
}
