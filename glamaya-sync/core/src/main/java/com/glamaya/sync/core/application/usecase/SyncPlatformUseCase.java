package com.glamaya.sync.core.application.usecase;

import com.glamaya.sync.core.domain.model.ProcessorType;

/**
 * Inbound port for triggering synchronization processes for a specific platform entity.
 * This interface defines how the core application logic can be driven by external actors
 * (e.g., a scheduler, a REST controller).
 */
public interface SyncPlatformUseCase {

    /**
     * Initiates the synchronization process for a given processor type.
     *
     * @param processorType The type of processor to run (e.g., WOOCOMMERCE_ORDER, WOOCOMMERCE_PRODUCT).
     */
    void sync(ProcessorType processorType);
}
