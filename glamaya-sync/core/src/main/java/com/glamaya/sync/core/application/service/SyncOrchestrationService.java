package com.glamaya.sync.core.application.service;

import com.glamaya.sync.core.application.usecase.SyncPlatformUseCase;
import com.glamaya.sync.core.domain.model.NotificationType;
import com.glamaya.sync.core.domain.model.ProcessorStatus;
import com.glamaya.sync.core.domain.model.ProcessorType;
import com.glamaya.sync.core.domain.model.SyncContext;
import com.glamaya.sync.core.domain.port.out.NotificationPort;
import com.glamaya.sync.core.domain.port.out.ProcessorConfiguration;
import com.glamaya.sync.core.domain.port.out.StatusStorePort;
import com.glamaya.sync.core.domain.port.out.SyncProcessor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class SyncOrchestrationService implements SyncPlatformUseCase {

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
            log.error("{}: Not configured for synchronization.", processorType);
            return Mono.empty();
        }
        return executeSync(processor);
    }

    private <P, C, T> Mono<Void> executeSync(SyncProcessor<P, C, T> processor) {
        ProcessorType processorType = processor.getProcessorType();
        ProcessorConfiguration<T> config = processor.getConfiguration();

        if (!config.isEnable()) {
            log.info("{}: Synchronization is disabled.", processorType);
            return Mono.empty();
        }

        // 1. Get the initial status
        Mono<ProcessorStatus> initialStatusMono = statusStorePort.findStatus(processorType)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .map(optStatus -> ProcessorStatus.fromConfiguration(processorType, optStatus.orElse(null), config));

        return initialStatusMono.flatMap(initialStatus -> {
            // Backoff state stored locally per run
            final int[] backoffAttempts = new int[]{0};
            final Duration passiveCap = toDuration(config.getFetchPassiveDelayMs(), 60_000L);
            final Duration activeDelay = toDuration(config.getFetchActiveDelayMs(), 0L);

            // Recursive function for fetching and processing pages with pacing
            Function<ProcessorStatus, Flux<P>> fetchAndProcessPage = new Function<>() {
                @Override
                public Flux<P> apply(ProcessorStatus currentStatus) {
                    if (!currentStatus.isMoreDataAvailable()) {
                        return Flux.empty(); // Stop recursion
                    }

                    log.info("{}: Fetching page {}, per-page {}.", processorType, currentStatus.getNextPage(), currentStatus.getPageSize());
                    SyncContext<T> syncContext = new SyncContext<>(currentStatus, config);

                    return processor.getDataProvider().fetchData(syncContext)
                            .collectList()
                            .flatMapMany(pageItems -> {
                                // Update status persistence and emit items
                                return statusStorePort.saveStatus(currentStatus)
                                        .thenMany(Flux.fromIterable(pageItems))
                                        .concatWith(Flux.defer(() -> {
                                            // Compute next delay
                                            Duration nextDelay = computeNextDelayUsingStatus(currentStatus, activeDelay, passiveCap, backoffAttempts);
                                            if (nextDelay != null && !nextDelay.isZero() && !nextDelay.isNegative()) {
                                                log.debug("{}: Delaying next page by {} ms (attempts={}).", processorType, nextDelay.toMillis(), backoffAttempts[0]);
                                                return Mono.delay(nextDelay).thenMany(this.apply(currentStatus));
                                            } else {
                                                return this.apply(currentStatus);
                                            }
                                        }));
                            });
                }
            };

            return fetchAndProcessPage.apply(initialStatus)
                    .flatMap(rawItem -> {
                        C canonicalModel = processor.getDataMapper().mapToCanonical(rawItem);
                        return Flux.fromArray(NotificationType.values())
                                .flatMap(type -> notificationPort.notify(canonicalModel, config, type))
                                .then(Mono.just(1));
                    })
                    .count() // Count total items processed
                    .flatMap(totalItems -> {
                        log.info("{}: Sync completed. Processed {} items in total.", processorType, totalItems);
                        initialStatus.setLastSuccessfulRun(Instant.now());
                        return statusStorePort.saveStatus(initialStatus); // Save final status
                    });
        }).then();
    }

    private Duration toDuration(Long ms, long defaultMs) {
        long val = ms != null ? ms : defaultMs;
        if (val <= 0) return Duration.ZERO;
        return Duration.ofMillis(val);
    }

    private Duration computeNextDelayUsingStatus(ProcessorStatus status, Duration activeDelay, Duration passiveCap, int[] backoffAttempts) {
        if (status.isMoreDataAvailable()) {
            // In active paging mode: prefer configured active delay if present; otherwise no delay.
            if (activeDelay != null && !activeDelay.isZero() && !activeDelay.isNegative()) {
                backoffAttempts[0] = 0; // reset backoff when actively paging
                return activeDelay;
            }
            backoffAttempts[0] = 0;
            return Duration.ZERO;
        }
        // No more data indicated: apply exponential backoff capped by passive cap.
        long initialMs = 250L;
        long nextMs = (long) (initialMs * Math.pow(2, Math.max(0, backoffAttempts[0])));
        backoffAttempts[0] = Math.min(backoffAttempts[0] + 1, 30);
        long capped = Math.min(nextMs, passiveCap.toMillis());
        return Duration.ofMillis(capped);
    }
}
