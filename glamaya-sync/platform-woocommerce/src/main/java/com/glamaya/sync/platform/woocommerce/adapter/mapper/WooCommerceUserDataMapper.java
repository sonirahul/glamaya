package com.glamaya.sync.platform.woocommerce.adapter.mapper;

import com.glamaya.datacontracts.ecommerce.Contact;
import com.glamaya.datacontracts.ecommerce.mapper.ContactMapperFactory;
import com.glamaya.datacontracts.woocommerce.User;
import com.glamaya.sync.core.domain.port.out.DataMapper;
import org.springframework.stereotype.Component;

/**
 * Implementation of DataMapper for converting WooCommerce User DTOs to Canonical User domain models.
 */
@Component // Mark as a Spring component for dependency injection
public class WooCommerceUserDataMapper implements DataMapper<User, Contact> {

    private final ContactMapperFactory<User> contactMapperFactory;

    public WooCommerceUserDataMapper(ContactMapperFactory<User> contactMapperFactory) {
        this.contactMapperFactory = contactMapperFactory;
    }

    @Override
    public Contact mapToCanonical(User platformModel) {
        // Use the injected UserMapperFactory to perform the conversion
        return contactMapperFactory.toGlamayaContact(platformModel, "woocommerce");
    }
}
