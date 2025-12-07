package com.glamaya.sync.runner.scheduler;

import com.glamaya.sync.core.application.usecase.SyncOrchestrator;
import com.glamaya.sync.core.domain.model.ExecutionMode;
import com.glamaya.sync.core.domain.port.out.PlatformAdapter;
import com.glamaya.sync.runner.common.LoggerConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;

/**
 * Scheduler that runs platforms sequentially; within each platform, processors run in parallel
 * with a configurable max concurrency cap.
 */
@Slf4j
@Component
public class SyncScheduler {

    private final SyncOrchestrator syncOrchestrator;
    private final List<PlatformAdapter> platformAdapters;
    private final int maxConcurrency;
    private final ExecutionMode executionMode;

    public SyncScheduler(SyncOrchestrator syncOrchestrator,
                         List<PlatformAdapter> platformAdapters,
                         @Value("${glamaya.sync.concurrency.max:0}") int maxConcurrency,
                         @Value("${glamaya.sync.execution.mode}") String executionMode) {
        this.syncOrchestrator = syncOrchestrator;
        this.platformAdapters = platformAdapters;
        this.maxConcurrency = maxConcurrency;
        this.executionMode = ExecutionMode.fromString(executionMode);
        log.info(LoggerConstants.SCHEDULER_INIT, platformAdapters.size(), this.maxConcurrency, this.executionMode);
    }

    /**
     * Scheduled task to run synchronization for all platforms sequentially.
     */
    @Scheduled(fixedDelayString = "${glamaya.sync.scheduler.fixedDelay:300000}")
    public void run() {
        log.info(LoggerConstants.SCHEDULER_START, executionMode, maxConcurrency);
        switch (executionMode) {
            case SEQUENTIAL -> runSequentialSync();
            case PLATFORM_PARALLEL -> runPlatformParallelSync();
            case PARALLEL -> runParallelSync();
        }
    }

    private void runSequentialSync() {
        Flux.fromIterable(sortedAdapters())
                .concatMap(adapter -> {
                    log.info(LoggerConstants.SCHEDULER_PLATFORM_START, adapter.getPlatformName(), 1, executionMode);
                    return Flux.fromIterable(adapter.getProcessorTypes())
                            .concatMap(syncOrchestrator::syncSequential)
                            .then()
                            .doOnSuccess(v -> log.info(LoggerConstants.SCHEDULER_PLATFORM_COMPLETE, adapter.getPlatformName(), 1, executionMode));
                })
                .doOnComplete(() -> log.info(LoggerConstants.SCHEDULER_ALL_COMPLETE, executionMode))
                .subscribe();
    }

    private void runPlatformParallelSync() {
        Flux.fromIterable(sortedAdapters())
                .concatMap(adapter -> {
                    log.info(LoggerConstants.SCHEDULER_PLATFORM_START, adapter.getPlatformName(), maxConcurrency, executionMode);
                    return syncOrchestrator.syncPlatformParallel(adapter.getProcessorTypes(), maxConcurrency)
                            .doOnSuccess(v -> log.info(LoggerConstants.SCHEDULER_PLATFORM_COMPLETE, adapter.getPlatformName(), maxConcurrency, executionMode));
                })
                .doOnComplete(() -> log.info(LoggerConstants.SCHEDULER_ALL_COMPLETE, executionMode))
                .subscribe();
    }

    private void runParallelSync() {
        log.info(LoggerConstants.SCHEDULER_PLATFORM_START, "All Platforms", maxConcurrency, executionMode);
        syncOrchestrator.syncParallel(maxConcurrency)
                .doOnSuccess(v -> log.info(LoggerConstants.SCHEDULER_ALL_COMPLETE, executionMode))
                .subscribe();
    }

    private List<PlatformAdapter> sortedAdapters() {
        return platformAdapters.stream()
                .sorted(Comparator.comparing(PlatformAdapter::getPlatformName))
                .toList();
    }
}
