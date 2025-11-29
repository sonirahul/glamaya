package com.glamaya.sync.core.domain.port.out;

/**
 * Minimal implementation of the ProcessorConfiguration interface that simply wraps
 * a typed configuration object.
 */
public class SimpleProcessorConfiguration<T> implements ProcessorConfiguration<T> {

    private final T config;

    public SimpleProcessorConfiguration(T config) {
        this.config = config;
    }

    @Override
    public T get() {
        return config;
    }
}

