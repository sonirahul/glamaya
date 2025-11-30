package com.glamaya.sync.core.domain.port.out;

import reactor.core.publisher.Mono;

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
     * Triggers the synchronization process for all supported entities (e.g., Orders, Products)
     * for this specific platform. The implementation will contain the logic to orchestrate
     * fetching, mapping, and publishing for its entities.
     *
     * @return A Mono<Void> that completes when all sync processes for the platform are finished.
     */
    Mono<Void> sync();
}
