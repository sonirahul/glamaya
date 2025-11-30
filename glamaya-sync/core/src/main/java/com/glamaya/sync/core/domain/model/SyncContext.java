package com.glamaya.sync.core.domain.model;

import com.glamaya.sync.core.domain.port.out.ProcessorConfiguration;

/**
 * A context object passed during a sync operation, holding the state
 * and configuration needed for the current run.
 */
public record SyncContext<T>(
        ProcessorStatus status,
        ProcessorConfiguration<T> configuration
) {
}
