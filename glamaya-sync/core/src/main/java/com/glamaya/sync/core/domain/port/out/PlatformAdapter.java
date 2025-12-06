package com.glamaya.sync.core.domain.port.out;

import com.glamaya.sync.core.domain.model.ProcessorType;

import java.util.List;

/**
 * Represents the main contract for a platform-specific integration.
 * Each platform module (e.g., platform-woocommerce) must provide an implementation of this interface.
 */
public interface PlatformAdapter {

    /**
     * Returns the unique name of the platform (e.g., "woocommerce", "wix").
     * This is used for logging, metrics, and configuration lookup.
     *
     * @return The platform's unique identifier.
     */
    String getPlatformName();

    /**
     * Returns the processor types owned by this platform.
     * Used by the orchestrator to run platform-scoped processors with concurrency.
     */
    List<ProcessorType> getProcessorTypes();
}
