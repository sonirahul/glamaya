package com.glamaya.sync.core.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * A class holding the state of a single sync process (e.g., for "WooCommerce Orders").
 * This object is used by the core logic and persisted by an adapter for the StatusStorePort.
 */
@Data
@NoArgsConstructor
public class ProcessorStatus {

    private ProcessorType processorType;
    private Instant lastSuccessfulRun;
    private String cursor; // For cursor-based pagination
    private int currentPage; // For page-based pagination
    private int pageSize;
    private int totalItemsSynced;

    public ProcessorStatus(ProcessorType processorType) {
        this.processorType = processorType;
    }
}
