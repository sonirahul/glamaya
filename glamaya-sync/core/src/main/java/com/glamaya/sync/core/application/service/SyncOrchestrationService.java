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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public Mono<Void> sync(ProcessorType processorType) {
        SyncProcessor<?, ?, ?> processor = syncProcessors.get(processorType);
        if (processor == null) {
            log.error("No SyncProcessor found for processor type: {}", processorType);
            return Mono.empty();
        }
        return executeSync(processor);
    }

    private <P, C, T> Mono<Void> executeSync(SyncProcessor<P, C, T> processor) {
        ProcessorType processorType = processor.getProcessorType();
        ProcessorConfiguration<T> config = processor.getConfiguration();

        if (!config.isEnable()) {
            log.info("Synchronization is disabled for processor type: {}", processorType);
            return Mono.empty();
        }

        // 1. Get the initial status
        Mono<ProcessorStatus> initialStatusMono = statusStorePort.findStatus(processorType)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .map(optStatus -> ProcessorStatus.fromConfiguration(processorType, optStatus.orElse(null), config));

        return initialStatusMono.flatMap(initialStatus -> {
            // Define the recursive function for fetching and processing pages
            // This function needs to be defined here to capture the generic types P, C, T
            // Using an anonymous inner class to satisfy definite assignment analysis for recursion
            Function<ProcessorStatus, Flux<P>> fetchAndProcessPage = new Function<>() {
                @Override
                public Flux<P> apply(ProcessorStatus currentStatus) {
                    if (!currentStatus.isMoreDataAvailable()) {
                        return Flux.empty(); // Stop recursion
                    }

                    log.info("Fetching page {} for processor type: {}", currentStatus.getNextPage(), processorType);
                    SyncContext<T> syncContext = new SyncContext<>(currentStatus, config);

                    return processor.getDataProvider().fetchData(syncContext)
                            .collectList()
                            .flatMapMany(pageItems -> statusStorePort.saveStatus(currentStatus)
                                    .thenMany(Flux.fromIterable(pageItems))
                                    .concatWith(Flux.defer(() -> this.apply(currentStatus))));
                }
            };

            return fetchAndProcessPage.apply(initialStatus)
                    .flatMap(rawItem -> {
                        C canonicalModel = processor.getDataMapper().mapToCanonical(rawItem);
                        return notificationPort.notify(canonicalModel).then(Mono.just(1)); // Emit 1 after notification
                    })
                    .count() // Count total items processed
                    .flatMap(totalItems -> {
                        log.info("Sync completed for {}. Processed {} items in total.", processorType, totalItems);
                        initialStatus.setLastSuccessfulRun(Instant.now());
                        return statusStorePort.saveStatus(initialStatus); // Save final status
                    });
        }).then();
    }
}
