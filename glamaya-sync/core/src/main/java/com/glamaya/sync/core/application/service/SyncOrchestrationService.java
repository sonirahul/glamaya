package com.glamaya.sync.core.application.service;

import com.glamaya.sync.core.application.usecase.SyncPlatformUseCase;
import com.glamaya.sync.core.domain.model.ProcessorStatus;
import com.glamaya.sync.core.domain.model.ProcessorType;
import com.glamaya.sync.core.domain.model.SyncContext;
import com.glamaya.sync.core.domain.port.out.NotificationPort;
import com.glamaya.sync.core.domain.port.out.StatusStorePort;
import com.glamaya.sync.core.domain.port.out.SyncProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SyncOrchestrationService implements SyncPlatformUseCase {

    private static final Logger log = LoggerFactory.getLogger(SyncOrchestrationService.class);

    private final StatusStorePort statusStorePort;
    private final NotificationPort<Object> notificationPort;
    private final Map<ProcessorType, SyncProcessor<?, ?>> syncProcessors;

    public SyncOrchestrationService(
            StatusStorePort statusStorePort,
            NotificationPort<Object> notificationPort,
            List<SyncProcessor<?, ?>> syncProcessors) {
        this.statusStorePort = statusStorePort;
        this.notificationPort = notificationPort;
        this.syncProcessors = syncProcessors.stream()
                .collect(Collectors.toMap(SyncProcessor::getProcessorType, Function.identity()));
    }

    @Override
    public void sync(ProcessorType processorType) {
        log.info("Starting synchronization for processor type: {}", processorType);

        SyncProcessor<?, ?> processor = syncProcessors.get(processorType);
        if (processor == null) {
            log.error("No SyncProcessor found for processor type: {}", processorType);
            return;
        }

        executeSync(processor);
    }

    private <P, C> void executeSync(SyncProcessor<P, C> processor) {
        ProcessorType processorType = processor.getProcessorType();
        ProcessorStatus currentStatus = statusStorePort.findStatus(processorType)
                .orElseGet(() -> new ProcessorStatus(processorType));

        Map<String, Object> configuration = Collections.emptyMap();
        boolean hasMoreData = true;
        int totalItemsProcessed = 0;

        // Start from page 1 if not specified, otherwise continue from where we left off.
        int currentPage = currentStatus.getCurrentPage() > 0 ? currentStatus.getCurrentPage() : 1;

        try {
            while (hasMoreData) {
                currentStatus.setCurrentPage(currentPage);
                SyncContext syncContext = new SyncContext(currentStatus, configuration);

                log.info("Fetching page {} for processor type: {}", currentPage, processorType);
                List<P> rawData = processor.getDataProvider().fetchData(syncContext);

                if (rawData == null || rawData.isEmpty()) {
                    log.info("No more data found for processor type: {}. Concluding sync.", processorType);
                    hasMoreData = false;
                } else {
                    log.debug("Fetched {} raw items on page {}.", rawData.size(), currentPage);
                    for (P rawItem : rawData) {
                        C canonicalModel = processor.getDataMapper().mapToCanonical(rawItem);
                        notificationPort.notify(canonicalModel);
                        totalItemsProcessed++;
                    }

                    // If the number of items returned is less than the page size, this was the last page.
                    if (rawData.size() < 100) { // Assuming a fixed page size of 100
                        hasMoreData = false;
                    } else {
                        currentPage++;
                    }
                }
            }

            // Sync completed successfully. Update status for the next run.
            currentStatus.setLastSuccessfulRun(Instant.now());
            currentStatus.setCurrentPage(1); // Reset to page 1 for the next sync cycle.
            statusStorePort.saveStatus(currentStatus);
            log.info("Synchronization completed successfully for processor type: {}. Processed {} items in total.", processorType, totalItemsProcessed);

        } catch (Exception e) {
            log.error("Error during synchronization for processor type {} on page {}: {}", processorType, currentPage, e.getMessage(), e);
            // Save the current page to resume from this point on the next run.
            currentStatus.setCurrentPage(currentPage);
            statusStorePort.saveStatus(currentStatus);
        }
    }
}
