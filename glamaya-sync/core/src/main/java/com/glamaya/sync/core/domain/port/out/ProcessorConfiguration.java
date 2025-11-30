package com.glamaya.sync.core.domain.port.out;

/**
 * Strongly-typed configuration contract returned by SyncProcessor implementations.
 * Allows modules to provide their own configuration objects while core treats it generically.
 */
public interface ProcessorConfiguration<T> {
    /** Returns the underlying typed configuration object. */
    T get();

    boolean isEnable();
    boolean isResetOnStartup();
    int getPageSize();
    int getCurrentPage();
    String getQueryUrl();
}
