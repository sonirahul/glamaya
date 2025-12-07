package com.glamaya.sync.core.application.service;

import com.glamaya.sync.core.application.usecase.SyncOrchestrator;
import com.glamaya.sync.core.common.LoggerConstants;
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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class SyncOrchestrationService implements SyncOrchestrator {

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
    public Mono<Void> syncSequential(ProcessorType processorType) {
        SyncProcessor<?, ?, ?> processor = syncProcessors.get(processorType);
        if (processor == null) {
            log.error(LoggerConstants.ORCH_NOT_CONFIGURED, processorType);
            return Mono.empty();
        }
        return executeSync(processor);
    }

    /**
     * Runs synchronization for all configured processors in parallel.
     *
     * @param maxConcurrency maximum number of processors to run in parallel; if <=0, defaults to size of processors map.
     */
    @Override
    public Mono<Void> syncParallel(int maxConcurrency) {
        int concurrency = maxConcurrency > 0 ? Math.min(maxConcurrency, syncProcessors.size()) : syncProcessors.size();
        log.info(LoggerConstants.ORCH_SYNC, "All Platforms", syncProcessors.size(), concurrency);
        syncProcessors.keySet().forEach(pt -> log.info(LoggerConstants.ORCH_REGISTERED, pt));
        return Flux.fromIterable(syncProcessors.values())
                .flatMap(proc -> {
                    ProcessorType pt = proc.getProcessorType();
                    log.info(LoggerConstants.ORCH_START_EXEC, pt);
                    return executeSync(proc)
                            .doOnSuccess(v -> log.info(LoggerConstants.ORCH_COMPLETE_EXEC, pt));
                }, concurrency)
                .then();
    }

    /**
     * Runs synchronization for a specific set of processor types (e.g., a platform) in parallel.
     */
    @Override
    public Mono<Void> syncPlatformParallel(List<ProcessorType> types, int maxConcurrency) {
        Set<ProcessorType> filter = Set.copyOf(types);
        int available = (int) syncProcessors.keySet().stream().filter(filter::contains).count();
        int concurrency = maxConcurrency > 0 ? Math.min(maxConcurrency, available) : available;
        log.info(LoggerConstants.ORCH_SYNC, "Platform processors", available, concurrency);
        return Flux.fromIterable(syncProcessors.entrySet())
                .filter(entry -> filter.contains(entry.getKey()))
                .flatMap(entry -> {
                    ProcessorType pt = entry.getKey();
                    SyncProcessor<?, ?, ?> proc = entry.getValue();
                    log.info(LoggerConstants.ORCH_START_EXEC, pt);
                    return executeSync(proc)
                            .doOnSuccess(v -> log.info(LoggerConstants.ORCH_COMPLETE_EXEC, pt));
                }, concurrency)
                .then();
    }

    private <P, C, T> Mono<Void> executeSync(SyncProcessor<P, C, T> processor) {
        ProcessorType processorType = processor.getProcessorType();
        log.info(LoggerConstants.ORCH_EXEC_INVOKED, processorType);
        ProcessorConfiguration<T> config = processor.getConfiguration();

        if (!config.isEnable()) {
            log.info(LoggerConstants.ORCH_SYNC_DISABLED, processorType);
            return Mono.empty();
        }

        // Build initial status from store + configuration
        Mono<ProcessorStatus> initialStatusMono = statusStorePort.findStatus(processorType)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .map(optStatus -> ProcessorStatus.fromConfiguration(processorType, optStatus.orElse(null), config));

        // Active delay to pace page fetches (0 means no delay)
        Duration activeDelay = toDuration(config.getFetchActiveDelayMs());

        return initialStatusMono.flatMap(initialStatus ->
                // Start recursive page processing
                processPages(processor, config, initialStatus, activeDelay)
                        // After all pages processed, notify for each item
                        .flatMap(rawItem -> {
                            C canonicalModel = processor.getDataMapper().mapToCanonical(rawItem);
                            return Flux.fromArray(NotificationType.values())
                                    .flatMap(type -> notificationPort.notify(canonicalModel, config, type))
                                    .then(Mono.just(1));
                        })
                        .count()
                        .flatMap(totalItems -> {
                            log.info(LoggerConstants.ORCH_SYNC_COMPLETED, processorType, totalItems);
                            initialStatus.setLastSuccessfulRun(Instant.now());
                            return statusStorePort.saveStatus(initialStatus);
                        })
        ).then();
    }

    private <P, T> Flux<P> processPages(SyncProcessor<P, ?, T> processor,
                                        ProcessorConfiguration<T> config,
                                        ProcessorStatus status,
                                        Duration activeDelay) {
        // Define recursive function: fetch -> persist -> emit -> delay -> recur
        Function<ProcessorStatus, Flux<P>> loop = new Function<>() {
            @Override
            public Flux<P> apply(ProcessorStatus current) {
                if (!current.isMoreDataAvailable()) {
                    return Flux.empty();
                }
                log.info(LoggerConstants.ORCH_FETCH_PAGE, processor.getProcessorType(), current.getNextPage(), current.getPageSize());
                SyncContext<T> ctx = new SyncContext<>(current, config);

                return processor.getDataProvider().fetchData(ctx)
                        .collectList()
                        .flatMapMany(items -> statusStorePort.saveStatus(current)
                                .thenMany(Flux.fromIterable(items))
                                .concatWith(Flux.defer(() -> {
                                    if (!activeDelay.isZero() && !activeDelay.isNegative()) {
                                        return Mono.delay(activeDelay).thenMany(this.apply(current));
                                    }
                                    return this.apply(current);
                                })));
            }
        };
        return loop.apply(status);
    }

    private Duration toDuration(Long ms) {
        if (ms == null || ms <= 0) return Duration.ZERO;
        return Duration.ofMillis(ms);
    }
}
