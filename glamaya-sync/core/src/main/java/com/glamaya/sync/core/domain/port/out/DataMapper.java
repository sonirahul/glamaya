package com.glamaya.sync.core.domain.port.out;

/**
 * An outbound port defining the contract for mapping a platform-specific model
 * to a canonical core domain model.
 *
 * @param <P> The Platform-specific model type (the source).
 * @param <C> The Canonical core model type (the destination).
 */
public interface DataMapper<P, C> {

    /**
     * Maps a single platform-specific object to its canonical representation.
     *
     * @param platformModel The raw object fetched from the platform's API.
     * @return The corresponding canonical domain model.
     */
    C mapToCanonical(P platformModel);
}
