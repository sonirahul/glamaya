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
            List<SyncProcessor<?, ?>> syncProcessors) { // Inject list of all SyncProcessors
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

        // Use a private helper method to handle generics safely
        executeSync(processor);
    }

    /**
     * Private helper method to execute the synchronization for a specific SyncProcessor,
     * preserving type safety.
     *
     * @param processor The SyncProcessor to execute.
     * @param <P> The platform-specific model type.
     * @param <C> The canonical core model type.
     */
    private <P, C> void executeSync(SyncProcessor<P, C> processor) {
        ProcessorType processorType = processor.getProcessorType();

        // 1. Retrieve current status
        ProcessorStatus currentStatus = statusStorePort.findStatus(processorType)
                .orElseGet(() -> new ProcessorStatus(processorType));

        // For now, let's assume configuration is empty or comes from elsewhere
        Map<String, Object> configuration = Collections.emptyMap(); // Placeholder

        SyncContext syncContext = new SyncContext(currentStatus, configuration);

        try {
            // 2. Fetch raw data using the DataProvider from the SyncProcessor
            List<P> rawData = processor.getDataProvider().fetchData(syncContext);
            log.debug("Fetched {} raw items for processor type: {}", rawData.size(), processorType);

            if (rawData.isEmpty()) {
                log.info("No new data to sync for processor type: {}", processorType);
                currentStatus.setLastSuccessfulRun(Instant.now());
                statusStorePort.saveStatus(currentStatus);
                return;
            }

            // 3. Map to canonical models and notify using the DataMapper from the SyncProcessor
            for (P rawItem : rawData) {
                C canonicalModel = processor.getDataMapper().mapToCanonical(rawItem);
                notificationPort.notify(canonicalModel);
            }

            // 4. Update and save status
            currentStatus.setLastSuccessfulRun(Instant.now());
            // TODO: Update cursor/page based on the last successfully processed item if applicable
            statusStorePort.saveStatus(currentStatus);
            log.info("Synchronization completed successfully for processor type: {}", processorType);

        } catch (Exception e) {
            log.error("Error during synchronization for processor type {}: {}", processorType, e.getMessage(), e);
            // Depending on policy, might save status with error or not update lastSuccessfulRun
        }
    }
}
