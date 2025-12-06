package com.glamaya.sync.platform.woocommerce.adapter;

import com.glamaya.sync.core.domain.model.ProcessorType;
import com.glamaya.sync.core.domain.port.out.DataMapper;
import com.glamaya.sync.core.domain.port.out.DataProvider;
import com.glamaya.sync.core.domain.port.out.ProcessorConfiguration;
import com.glamaya.sync.core.domain.port.out.SyncProcessor;
import com.glamaya.sync.platform.woocommerce.config.APIConfig;

/**
 * Abstract base processor to reduce duplication across WooCommerce processors.
 * Provides common wiring for DataProvider, DataMapper, ProcessorType, and configuration.
 */
public abstract class AbstractWooCommerceProcessor<S, T> implements SyncProcessor<S, T, APIConfig> {

    private final DataProvider<S> dataProvider;
    private final DataMapper<S, T> dataMapper;
    private final ProcessorType processorType;
    private final ProcessorConfiguration<APIConfig> configuration;

    protected AbstractWooCommerceProcessor(
            DataProvider<S> dataProvider,
            DataMapper<S, T> dataMapper,
            ProcessorType processorType,
            ProcessorConfiguration<APIConfig> configuration) {
        this.dataProvider = dataProvider;
        this.dataMapper = dataMapper;
        this.processorType = processorType;
        this.configuration = configuration;
    }

    @Override
    public DataProvider<S> getDataProvider() {
        return dataProvider;
    }

    @Override
    public DataMapper<S, T> getDataMapper() {
        return dataMapper;
    }

    @Override
    public ProcessorType getProcessorType() {
        return processorType;
    }

    @Override
    public ProcessorConfiguration<APIConfig> getConfiguration() {
        return configuration;
    }
}

