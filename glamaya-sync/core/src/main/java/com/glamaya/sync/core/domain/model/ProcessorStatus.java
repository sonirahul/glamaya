package com.glamaya.sync.core.domain.model;

import com.glamaya.sync.core.domain.port.out.ProcessorConfiguration;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * A class holding the state of a single sync process (e.g., for "WooCommerce Orders").
 * This object is used by the core logic and persisted by an adapter for the StatusStorePort.
 */
@Data
@SuperBuilder
@NoArgsConstructor
public class ProcessorStatus {

    private ProcessorType processorType;
    private Instant lastSuccessfulRun;
    private Instant lastDateModified;
    private boolean useLastDateModifiedInQuery;
    private String cursor; // For cursor-based pagination
    private Integer nextPage; // For page-based pagination
    private Integer pageSize;
    private boolean moreDataAvailable;
    private Integer totalItemsSynced;

    /**
     * Factory method to create a new status initialized from configuration.
     * Uses initPage and pageSize if available; defaults are caller-defined.
     */
    public static ProcessorStatus fromConfiguration(ProcessorType type, ProcessorStatus status, ProcessorConfiguration<?> configuration) {

        if (status == null || configuration.isResetOnStartup()) {
            return ProcessorStatus.builder()
                    .processorType(type)
                    .lastDateModified(null)
                    .useLastDateModifiedInQuery(false)
                    .cursor(null)
                    .nextPage(configuration.getInitPage())
                    .pageSize(configuration.getPageSize())
                    .moreDataAvailable(true)
                    .totalItemsSynced(0)
                    .build();
        }

        status.setMoreDataAvailable(true);
        return status;
    }
}
