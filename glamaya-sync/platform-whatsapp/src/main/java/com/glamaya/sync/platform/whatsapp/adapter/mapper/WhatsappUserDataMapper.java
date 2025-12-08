package com.glamaya.sync.platform.whatsapp.adapter.mapper;

import com.glamaya.datacontracts.ecommerce.Contact;
import com.glamaya.datacontracts.ecommerce.mapper.ContactMapperFactory;
import com.glamaya.datacontracts.whatsapp.Chat;
import com.glamaya.sync.core.domain.model.EcomModel;
import com.glamaya.sync.core.domain.port.out.DataMapper;
import org.springframework.stereotype.Component;

import static com.glamaya.sync.platform.whatsapp.common.Constants.PLATFORM_NAME;

/**
 * Implementation of DataMapper for converting WhatsApp Chat DTOs to Canonical User domain models.
 */
@Component // Mark as a Spring component for dependency injection
public class WhatsappUserDataMapper implements DataMapper<Chat, EcomModel<Contact>> {

    private final ContactMapperFactory<Chat> contactMapperFactory;

    /**
     * Constructs the WhatsappUserDataMapper with the required ContactMapperFactory.
     *
     * @param contactMapperFactory The factory for mapping Chat to Contact.
     */
    public WhatsappUserDataMapper(ContactMapperFactory<Chat> contactMapperFactory) {
        this.contactMapperFactory = contactMapperFactory;
    }

    /**
     * Maps a WhatsApp Chat DTO to a canonical Contact domain model.
     *
     * @param platformModel The WhatsApp Chat DTO.
     * @return The canonical EcomModel<Contact> or null if mapping fails.
     */
    @Override
    public EcomModel<Contact> mapToCanonical(Chat platformModel) {
        // Use the injected ContactMapperFactory to perform the conversion
        var contact = contactMapperFactory.toGlamayaContact(platformModel, PLATFORM_NAME);
        return contact != null ? new EcomModel<>(contact.getId(), contact) : null;
    }
}
