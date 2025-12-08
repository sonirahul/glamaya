package com.glamaya.sync.platform.whatsapp.adapter;

import com.glamaya.sync.core.domain.model.ProcessorType;
import com.glamaya.sync.core.domain.port.out.PlatformAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.glamaya.sync.platform.whatsapp.common.Constants.PLATFORM_NAME;

/**
 * WhatsApp-specific implementation of the PlatformAdapter outbound port.
 * This adapter orchestrates the synchronization of WhatsApp entities
 * by delegating to the core SyncPlatformUseCase.
 */
@Slf4j
@Component
public class WhatsappPlatformAdapter implements PlatformAdapter {

    /**
     * Returns the platform name for WhatsApp integration.
     * @return The platform name constant.
     */
    @Override
    public String getPlatformName() {
        return PLATFORM_NAME;
    }

    /**
     * Returns the list of supported processor types for WhatsApp.
     * @return List of ProcessorType.WHATSAPP
     */
    @Override
    public List<ProcessorType> getProcessorTypes() {
        return List.of(ProcessorType.WHATSAPP);
    }
}
