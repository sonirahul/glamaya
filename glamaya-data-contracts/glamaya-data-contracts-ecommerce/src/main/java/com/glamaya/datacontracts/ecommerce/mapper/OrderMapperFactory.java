package com.glamaya.datacontracts.ecommerce.mapper;

import com.glamaya.datacontracts.ecommerce.Order;

/**
 * Factory interface to convert a platform-specific order into unified ecommerce Order.
 * Similar pattern to ContactMapperFactory.
 */
public interface OrderMapperFactory<S> {
    Order toGlamayaOrder(S source, String sourceAccountName);
}

