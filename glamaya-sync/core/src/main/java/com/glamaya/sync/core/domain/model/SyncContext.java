package com.glamaya.sync.core.domain.model;

import java.util.Map;

/**
 * A context object passed during a sync operation, holding the state
 * and configuration needed for the current run.
 *
 * @param status The current status of the processor (e.g., last run date, cursor).
 * @param configuration A map of platform-specific configuration properties.
 */
public record SyncContext(
    ProcessorStatus status,
    Map<String, Object> configuration
) {}
