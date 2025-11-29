package com.glamaya.sync.core.domain.model;

import com.glamaya.sync.core.domain.port.out.ProcessorConfiguration;

/**
 * A context object passed during a sync operation, holding the state
 * and configuration needed for the current run.
 *
 * @param status The current status of the processor (e.g., last run date, cursor).
 * @param configuration A typed configuration object for the processor.
 */
public record SyncContext(
    ProcessorStatus status,
    ProcessorConfiguration<?> configuration
) {}
