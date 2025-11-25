package com.glamaya.glamayawoocommercesync.port.out;

import com.glamaya.glamayawoocommercesync.domain.ProcessorStatus;
import com.glamaya.glamayawoocommercesync.domain.ProcessorType;
import reactor.core.publisher.Mono;

/**
 * Defines the outbound port for persisting and retrieving {@link ProcessorStatus} information.
 * This interface abstracts the details of the underlying persistence mechanism,
 * allowing the application core to remain decoupled from the database technology.
 */
public interface StatusTrackerStore {
    /**
     * Retrieves an existing {@link ProcessorStatus} or creates a new one if it doesn't exist.
     *
     * @param type           The {@link ProcessorType} to retrieve/create the status for.
     * @param resetOnStartup Whether to reset the tracker's state if it's the first time it's being accessed on startup.
     * @param pageSize       The configured page size for the processor.
     * @return A {@link Mono} emitting the {@link ProcessorStatus}.
     */
    Mono<ProcessorStatus> getOrCreate(ProcessorType type, boolean resetOnStartup, long pageSize);

    /**
     * Saves or updates a {@link ProcessorStatus}.
     *
     * @param tracker The {@link ProcessorStatus} to save.
     * @return A {@link Mono} emitting the saved {@link ProcessorStatus}.
     */
    Mono<ProcessorStatus> save(ProcessorStatus tracker);
}
