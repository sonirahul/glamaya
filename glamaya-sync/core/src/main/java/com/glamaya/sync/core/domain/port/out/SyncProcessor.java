package com.glamaya.sync.core.domain.port.out;

import com.glamaya.sync.core.domain.model.ProcessorType;

/**
 * A port that represents a self-contained processing unit for a specific data type.
 * It encapsulates the data fetching and mapping logic, preserving the generic type link
 * between the platform model and the canonical model.
 *
 * @param <P> The Platform-specific model type.
 * @param <C> The Canonical core model type.
 */
public interface SyncProcessor<P, C> {

    /**
     * Returns the data provider for the platform-specific model.
     *
     * @return The DataProvider instance.
     */
    DataProvider<P> getDataProvider();

    /**
     * Returns the data mapper to convert the platform model to the canonical model.
     *
     * @return The DataMapper instance.
     */
    DataMapper<P, C> getDataMapper();

    /**
     * Returns the unique processor type this processor handles.
     *
     * @return The ProcessorType enum.
     */
    ProcessorType getProcessorType();
}
