package com.glamaya.glamayawoocommercesync.application.service;

import com.glamaya.glamayawoocommercesync.domain.ProcessorType; // Updated import
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.scheduling.PollerMetadata;

public interface GlamWoocommerceProcessor<T> extends GenericHandler<T> {

    MessageSource<T> receive();

    PollerMetadata getPoller();

    ProcessorType getProcessorType();
}
