package com.glamaya.sync.platform.woocommerce.adapter.mapper;

import com.glamaya.datacontracts.ecommerce.Contact;
import com.glamaya.datacontracts.ecommerce.mapper.ContactMapperFactory;
import com.glamaya.datacontracts.woocommerce.Order;
import com.glamaya.sync.core.domain.model.EcomModel;
import com.glamaya.sync.core.domain.port.out.DataMapper;
import org.springframework.stereotype.Component;

import static com.glamaya.sync.platform.woocommerce.common.Constants.PLATFORM_NAME;

/**
 * Implementation of DataMapper for converting WooCommerce Order DTOs to Canonical Order domain models.
 */
@Component // Mark as a Spring component for dependency injection
public class WooCommerceOrderToContactDataMapper
        implements DataMapper<Order, EcomModel<Contact>> {

    private final ContactMapperFactory<Order> contactMapperFactory;

    public WooCommerceOrderToContactDataMapper(ContactMapperFactory<Order> contactMapperFactory) {
        this.contactMapperFactory = contactMapperFactory;
    }

    @Override
    public EcomModel<Contact> mapToCanonical(Order platformModel) {
        // Use the injected OrderMapperFactory to perform the conversion
        var contact = contactMapperFactory.toGlamayaContact(platformModel, PLATFORM_NAME);
        return new EcomModel<>(contact.getId(), contact);
    }
}
