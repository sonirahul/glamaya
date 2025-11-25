package com.glamaya.sync.core.domain.port.out;

import com.glamaya.sync.core.domain.model.ProcessorStatus;
import com.glamaya.sync.core.domain.model.ProcessorType;
import java.util.Optional;

/**
 * An outbound port for persisting and retrieving the status of a synchronization process.
 */
public interface StatusStorePort {

    /**
     * Finds the last saved status for a given processor type.
     *
     * @param processorType The unique identifier for the processor (e.g., WOOCOMMERCE_ORDER).
     * @return An Optional containing the ProcessorStatus if found, otherwise empty.
     */
    Optional<ProcessorStatus> findStatus(ProcessorType processorType);

    /**
     * Saves the current status of a processor.
     *
     * @param status The ProcessorStatus object to save.
     */
    void saveStatus(ProcessorStatus status);
}
