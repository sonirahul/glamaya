package com.glamaya.glamayawoocommercesync.processor;

import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorType;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.scheduling.PollerMetadata;

public interface GlamWoocommerceProcessor<T> extends GenericHandler<T> {

    MessageSource<T> receive();

    PollerMetadata getPoller();

    ProcessorType getProcessorType();
}