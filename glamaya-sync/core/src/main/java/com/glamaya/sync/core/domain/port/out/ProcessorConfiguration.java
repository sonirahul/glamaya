package com.glamaya.sync.core.domain.port.out;

import com.glamaya.sync.core.domain.model.ProcessorType;

/**
 * Strongly-typed configuration contract returned by SyncProcessor implementations.
 * Allows modules to provide their own configuration objects while core treats it generically.
 */
public interface ProcessorConfiguration<T> {
    /** Returns the underlying typed configuration object. */
    T get();

    ProcessorType getProcessorType();
    boolean isEnable();
    boolean isResetOnStartup();
    int getInitPage();
    int getPageSize();
    String getQueryUrl();
}
