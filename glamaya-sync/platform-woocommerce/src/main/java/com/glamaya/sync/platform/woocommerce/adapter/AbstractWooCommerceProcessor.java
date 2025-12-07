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
public abstract class AbstractWooCommerceProcessor<P, C> implements SyncProcessor<P, C, APIConfig> {

    private final DataProvider<P> dataProvider;
    private final DataMapper<P, C> dataMapper;
    private final ProcessorType processorType;
    private final ProcessorConfiguration<APIConfig> configuration;

    protected AbstractWooCommerceProcessor(
            DataProvider<P> dataProvider,
            DataMapper<P, C> dataMapper,
            ProcessorType processorType,
            ProcessorConfiguration<APIConfig> configuration) {
        this.dataProvider = dataProvider;
        this.dataMapper = dataMapper;
        this.processorType = processorType;
        this.configuration = configuration;
    }

    @Override
    public DataProvider<P> getDataProvider() {
        return dataProvider;
    }

    @Override
    public DataMapper<P, C> getDataMapper() {
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

