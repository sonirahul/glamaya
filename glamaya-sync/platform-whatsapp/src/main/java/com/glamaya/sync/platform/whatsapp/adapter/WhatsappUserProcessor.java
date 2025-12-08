package com.glamaya.sync.platform.whatsapp.adapter;

import com.glamaya.datacontracts.ecommerce.Contact;
import com.glamaya.datacontracts.whatsapp.Chat;
import com.glamaya.sync.core.domain.model.EcomModel;
import com.glamaya.sync.core.domain.model.ProcessorType;
import com.glamaya.sync.platform.whatsapp.adapter.client.WhatsappUserDataProvider;
import com.glamaya.sync.platform.whatsapp.adapter.mapper.WhatsappUserDataMapper;
import com.glamaya.sync.platform.whatsapp.config.WhatsappEndpointConfiguration;
import org.springframework.stereotype.Component;

/**
 * Concrete implementation of SyncProcessor for WhatsApp Users.
 * This class groups the DataProvider, DataMapper, and configuration for WhatsApp Users,
 * making them available to the SyncOrchestrationService in a type-safe manner.
 */
@Component
public class WhatsappUserProcessor extends AbstractWhatsappProcessor<Chat, EcomModel<Contact>> {

    /**
     * Constructs the WhatsappUserProcessor with required dependencies.
     *
     * @param dataProvider   The data provider for WhatsApp user data.
     * @param dataMapper     The data mapper for WhatsApp user data.
     * @param configProvider The endpoint configuration provider.
     */
    public WhatsappUserProcessor(
            WhatsappUserDataProvider dataProvider,
            WhatsappUserDataMapper dataMapper,
            WhatsappEndpointConfiguration configProvider) {
        super(
                dataProvider,
                dataMapper,
                ProcessorType.WHATSAPP,
                configProvider.getConfiguration(ProcessorType.WHATSAPP)
        );
    }
}
