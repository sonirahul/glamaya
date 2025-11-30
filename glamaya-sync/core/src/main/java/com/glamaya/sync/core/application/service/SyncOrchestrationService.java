package com.glamaya.sync.core.application.service;

import com.glamaya.sync.core.application.usecase.SyncPlatformUseCase;
import com.glamaya.sync.core.domain.model.ProcessorStatus;
import com.glamaya.sync.core.domain.model.ProcessorType;
import com.glamaya.sync.core.domain.model.SyncContext;
import com.glamaya.sync.core.domain.port.out.NotificationPort;
import com.glamaya.sync.core.domain.port.out.ProcessorConfiguration;
import com.glamaya.sync.core.domain.port.out.StatusStorePort;
import com.glamaya.sync.core.domain.port.out.SyncProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SyncOrchestrationService implements SyncPlatformUseCase {

    private static final Logger log = LoggerFactory.getLogger(SyncOrchestrationService.class);

    private final StatusStorePort statusStorePort;
    private final NotificationPort<Object> notificationPort;
    private final Map<ProcessorType, SyncProcessor<?, ?, ?>> syncProcessors;

    public SyncOrchestrationService(
            StatusStorePort statusStorePort,
            NotificationPort<Object> notificationPort,
            List<SyncProcessor<?, ?, ?>> syncProcessors) {
        this.statusStorePort = statusStorePort;
        this.notificationPort = notificationPort;
        this.syncProcessors = syncProcessors.stream()
                .collect(Collectors.toMap(SyncProcessor::getProcessorType, Function.identity()));
    }

    @Override
    public void sync(ProcessorType processorType) {
        SyncProcessor<?, ?, ?> processor = syncProcessors.get(processorType);
        if (processor == null) {
            log.error("No SyncProcessor found for processor type: {}", processorType);
            return;
        }

        executeSync(processor);
    }

    private <P, C, T> void executeSync(SyncProcessor<P, C, T> processor) {
        ProcessorType processorType = processor.getProcessorType();
        ProcessorConfiguration<T> config = processor.getConfiguration();

        if (config.isEnable()) {

            log.info("Starting synchronization for processor type: {}", processorType);
            ProcessorStatus status = ProcessorStatus
                    .fromConfiguration(processorType, statusStorePort.findStatus(processorType).orElse(null), config);

            int totalItemsProcessed = 0;

            try {
                SyncContext<T> syncContext = new SyncContext<>(status, config);
                while (status.isMoreDataAvailable()) {
                    log.info("Fetching page {} for processor type: {}", status.getNextPage(), processorType);
                    List<P> rawData = processor.getDataProvider().fetchData(syncContext);

                    if (rawData == null || rawData.isEmpty()) {
                        log.info("No more data found for processor type: {}. Concluding sync.", processorType);
                    } else {
                        log.debug("Fetched {} raw items on page {}.", rawData.size(), status.getNextPage());
                        for (P rawItem : rawData) {
                            C canonicalModel = processor.getDataMapper().mapToCanonical(rawItem);
                            notificationPort.notify(canonicalModel);
                            totalItemsProcessed++;
                        }
                    }
                    status.setLastSuccessfulRun(Instant.now());
                    statusStorePort.saveStatus(status);
                }

                log.info("Synchronization completed successfully for processor type: {}. Processed {} items in total.", processorType, totalItemsProcessed);
            } catch (Exception e) {
                log.error("Error during synchronization for processor type {} on page {}: {}", processorType, status.getNextPage(), e.getMessage(), e);
                status.setNextPage(status.getNextPage());
                statusStorePort.saveStatus(status);
            }
        } else {
            log.info("Synchronization is disabled for processor type: {}", processorType);
        }
    }
}
