package com.glamaya.sync.platform.woocommerce.adapter.client.descriptor;

import com.glamaya.datacontracts.woocommerce.Order;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import static com.glamaya.sync.platform.woocommerce.adapter.util.WooDateParsingUtils.PARSE_ISO_LOCAL_DATE_TIME_TO_INSTANT;

/**
 * The concrete descriptor for fetching WooCommerce Order entities.
 */
@Component
public class OrderDescriptor implements WooCommerceEntityDescriptor<Order> {

    private static final ParameterizedTypeReference<List<Order>> ORDER_LIST_TYPE = new ParameterizedTypeReference<>() {
    };

    @Override
    public ParameterizedTypeReference<List<Order>> getListTypeReference() {
        return ORDER_LIST_TYPE;
    }

    @Override
    public Function<Order, Instant> getLastModifiedExtractor() {
        return order -> PARSE_ISO_LOCAL_DATE_TIME_TO_INSTANT.apply(order.getDateModifiedGmt());
    }
}
