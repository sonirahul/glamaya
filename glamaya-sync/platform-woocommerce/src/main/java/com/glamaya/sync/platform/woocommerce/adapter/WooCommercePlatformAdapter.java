package com.glamaya.sync.platform.woocommerce.adapter;

import com.glamaya.sync.core.domain.model.ProcessorType;
import com.glamaya.sync.core.domain.port.out.PlatformAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WooCommerce-specific implementation of the PlatformAdapter outbound port.
 * This adapter orchestrates the synchronization of various WooCommerce entities
 * by delegating to the core SyncPlatformUseCase.
 */
@Slf4j
@Component
public class WooCommercePlatformAdapter implements PlatformAdapter {

    @Override
    public String getPlatformName() {
        return "woocommerce";
    }

    @Override
    public List<ProcessorType> getProcessorTypes() {
        return List.of(ProcessorType.WOOCOMMERCE_ORDER, ProcessorType.WOOCOMMERCE_USER);
    }
}
