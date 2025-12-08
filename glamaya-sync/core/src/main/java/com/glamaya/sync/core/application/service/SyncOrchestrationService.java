package com.glamaya.sync.core.application.service;

import com.glamaya.sync.core.application.usecase.SyncOrchestrator;
import com.glamaya.sync.core.common.LoggerConstants;
import com.glamaya.sync.core.domain.model.EcomModel;
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
    private final NotificationPort<EcomModel<?>> notificationPort;
    private final Map<ProcessorType, SyncProcessor<?, ?, ?>> syncProcessors;

    /**
     * Constructs the SyncOrchestrationService with required ports and processors.
     *
     * @param statusStorePort   Port for storing and retrieving processor status.
     * @param notificationPort  Port for sending notifications for canonical models.
     * @param syncProcessors    List of all available sync processors.
     */
    public SyncOrchestrationService(
            StatusStorePort statusStorePort,
            NotificationPort<EcomModel<?>> notificationPort,
            List<SyncProcessor<?, ?, ?>> syncProcessors) {
        this.statusStorePort = statusStorePort;
        this.notificationPort = notificationPort;
        // Map processors by their type for quick lookup
        this.syncProcessors = syncProcessors.stream()
                .collect(Collectors.toMap(SyncProcessor::getProcessorType, Function.identity()));
    }

    /**
     * Runs synchronization for a single processor type sequentially.
     *
     * @param processorType The processor type to synchronize.
     * @return Mono signaling completion.
     */
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
     * @param maxConcurrency Maximum number of processors to run in parallel; if <=0, defaults to size of processors map.
     * @return Mono signaling completion.
     */
    @Override
    public Mono<Void> syncParallel(int maxConcurrency) {
        int concurrency = maxConcurrency > 0 ? Math.min(maxConcurrency, syncProcessors.size()) : syncProcessors.size();
        log.info(LoggerConstants.ORCH_SYNC, "All Platforms", syncProcessors.size(), concurrency);
        syncProcessors.keySet().forEach(pt -> log.info(LoggerConstants.ORCH_REGISTERED, pt));
        // Run all processors in parallel with the specified concurrency
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
     *
     * @param types          List of processor types to synchronize.
     * @param maxConcurrency Maximum number of processors to run in parallel.
     * @return Mono signaling completion.
     */
    @Override
    public Mono<Void> syncPlatformParallel(List<ProcessorType> types, int maxConcurrency) {
        Set<ProcessorType> filter = Set.copyOf(types);
        int available = (int) syncProcessors.keySet().stream().filter(filter::contains).count();
        int concurrency = maxConcurrency > 0 ? Math.min(maxConcurrency, available) : available;
        log.info(LoggerConstants.ORCH_SYNC, "Platform processors", available, concurrency);
        // Run only the filtered processors in parallel
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

    /**
     * Executes the synchronization process for a given processor.
     * Handles status initialization, page fetching, mapping, notification, and status update.
     *
     * @param processor The sync processor to execute.
     * @param <P>       Raw data item type.
     * @param <C>       Canonical model type.
     * @param <T>       Processor configuration type.
     * @return Mono signaling completion.
     */
    private <P, C extends EcomModel<?>, T> Mono<Void> executeSync(SyncProcessor<P, C, T> processor) {
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
                fetchPagedData(processor, config, initialStatus, activeDelay)
                        // After all pages processed, notify for each item
                        .flatMap(rawItem -> {
                            C canonicalModel = mapToCanonical(rawItem, processor, processorType);
                            // Only notify if mapping succeeded
                            return canonicalModel == null? Mono.empty() : notifyAll(canonicalModel, config);
                        })
                        .count()
                        .flatMap(totalItems -> {
                            log.info(LoggerConstants.ORCH_SYNC_COMPLETED, processorType, totalItems);
                            initialStatus.setLastSuccessfulRun(Instant.now());
                            return statusStorePort.saveStatus(initialStatus);
                        })
        ).then();
    }

    /**
     * Recursively fetches and emits data items page by page from the given processor,
     * saving the status after each page and optionally applying a delay between fetches.
     * The recursion stops when no more data is available.
     *
     * @param processor   The sync processor responsible for data fetching and mapping.
     * @param config      The processor configuration containing fetch and paging settings.
     * @param status      The current processor status, including paging information.
     * @param activeDelay The delay to apply between page fetches; zero means no delay.
     * @param <P>         Raw data item type returned by the processor.
     * @param <C>         Canonical model type (not used in this method).
     * @param <T>         Processor configuration type.
     * @return Flux emitting all raw data items fetched across all pages.
     */
    private <P, C extends EcomModel<?>, T> Flux<P> fetchPagedData(SyncProcessor<P, C, T> processor,
                                                                  ProcessorConfiguration<T> config,
                                                                  ProcessorStatus status,
                                                                  Duration activeDelay) {
        // Recursive function: fetch -> emit -> save -> optional delay -> recur
        Function<ProcessorStatus, Flux<P>> loop = new Function<>() {
            @Override
            public Flux<P> apply(ProcessorStatus current) {
                if (!current.isMoreDataAvailable()) {
                    return Flux.empty();
                }
                log.info(LoggerConstants.ORCH_FETCH_PAGE, processor.getProcessorType(), current.getNextPage(), current.getPageSize());
                SyncContext<T> ctx = new SyncContext<>(current, config);

                Flux<P> pageFlux = processor.getDataProvider().fetchData(ctx);

                return pageFlux
                        .collectList()
                        .flatMapMany(items -> statusStorePort.saveStatus(current)
                                .thenMany(Flux.fromIterable(items)))
                        .concatWith(Flux.defer(() -> {
                            if (!current.isMoreDataAvailable()) {
                                return Flux.empty();
                            }
                            // Apply delay if configured before next page fetch
                            if (!activeDelay.isZero() && !activeDelay.isNegative()) {
                                return Mono.delay(activeDelay).thenMany(this.apply(current));
                            }
                            return this.apply(current);
                        }));
            }
        };
        return loop.apply(status);
    }

    /**
     * Notifies all notification types for the given canonical model and configuration.
     *
     * @param canonicalModel The canonical model to notify about.
     * @param config         The processor configuration.
     * @param <C>            Canonical model type.
     * @param <T>            Processor configuration type.
     * @return Mono emitting 1 after all notifications are sent.
     */
    private <C extends EcomModel<?>, T> Mono<Integer> notifyAll(C canonicalModel, ProcessorConfiguration<T> config) {
        // Notify for each NotificationType
        return Flux.fromArray(NotificationType.values())
                .flatMap(type -> notificationPort.notify(canonicalModel, config, type))
                .then(Mono.just(1));
    }

    /**
     * Maps a raw item to its canonical model, logging errors if mapping fails or returns null.
     *
     * @param rawItem        The raw data item to map.
     * @param processor      The sync processor providing the mapper.
     * @param processorType  The processor type (for logging).
     * @param <P>            Raw data item type.
     * @param <C>            Canonical model type.
     * @param <T>            Processor configuration type.
     * @return The canonical model, or null if mapping fails.
     */
    private <P, C extends EcomModel<?>, T> C mapToCanonical(P rawItem, SyncProcessor<P, C, T> processor, ProcessorType processorType) {
        try {
            C canonicalModel = processor.getDataMapper().mapToCanonical(rawItem);
            if (canonicalModel == null) {
                log.error("Canonical mapping returned null for processor: {} item: {}", processorType, rawItem);
            }
            return canonicalModel;
        } catch (Exception e) {
            log.error("Canonical mapping failed for processor: {} item: {} error: {}", processorType, rawItem, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Converts a millisecond value to a Duration, returning zero if null or non-positive.
     *
     * @param ms Milliseconds to convert.
     * @return Duration representing the given milliseconds, or zero if invalid.
     */
    private Duration toDuration(Long ms) {
        if (ms == null || ms <= 0) return Duration.ZERO;
        return Duration.ofMillis(ms);
    }
}
