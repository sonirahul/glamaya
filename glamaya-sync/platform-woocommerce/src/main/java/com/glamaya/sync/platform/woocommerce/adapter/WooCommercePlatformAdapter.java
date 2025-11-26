package com.glamaya.sync.platform.woocommerce.adapter;

import com.glamaya.sync.core.application.usecase.SyncPlatformUseCase;
import com.glamaya.sync.core.domain.model.ProcessorType;
import com.glamaya.sync.core.domain.port.out.PlatformAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * WooCommerce-specific implementation of the PlatformAdapter outbound port.
 * This adapter orchestrates the synchronization of various WooCommerce entities
 * by delegating to the core SyncPlatformUseCase.
 */
@Component
public class WooCommercePlatformAdapter implements PlatformAdapter {

    private static final Logger log = LoggerFactory.getLogger(WooCommercePlatformAdapter.class);
    private final SyncPlatformUseCase syncPlatformUseCase;

    public WooCommercePlatformAdapter(SyncPlatformUseCase syncPlatformUseCase) {
        this.syncPlatformUseCase = syncPlatformUseCase;
    }

    @Override
    public String getPlatformName() {
        return "woocommerce";
    }

    @Override
    public void sync() {
        log.info("Initiating full synchronization for WooCommerce platform.");

        // Trigger synchronization for WooCommerce Orders
        log.debug("Triggering sync for WooCommerce Orders...");
        syncPlatformUseCase.sync(ProcessorType.WOOCOMMERCE_ORDER);

        // TODO: Add calls for other WooCommerce entities as they are implemented
        // syncPlatformUseCase.sync(ProcessorType.WOOCOMMERCE_PRODUCT);
        // syncPlatformUseCase.sync(ProcessorType.WOOCOMMERCE_USER);

        log.info("Completed full synchronization for WooCommerce platform.");
    }
}
