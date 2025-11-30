package com.glamaya.sync.runner.scheduler;

import com.glamaya.sync.core.domain.port.out.PlatformAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * A scheduler component that periodically triggers synchronization processes
 * for all registered PlatformAdapters.
 */
@Component
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);
    private final List<PlatformAdapter> platformAdapters;

    public SyncScheduler(List<PlatformAdapter> platformAdapters) {
        this.platformAdapters = platformAdapters;
        log.info("SyncScheduler initialized with {} platform adapters.", platformAdapters.size());
    }

    /**
     * Scheduled task to run synchronization for all active platforms.
     * Runs at a fixed delay, ensuring one execution finishes before the next one starts.
     */
    @Scheduled(fixedDelayString = "${glamaya.sync.scheduler.fixedDelay:300000}")
    public void runAllPlatformSyncs() {
        log.info("Starting scheduled synchronization for all platforms.");
        Flux.fromIterable(platformAdapters)
                .concatMap(adapter -> adapter.sync()
                        .doOnSubscribe(s -> log.info("Running sync for platform: {}", adapter.getPlatformName()))
                        .doOnSuccess(v -> log.info("Sync completed for platform: {}", adapter.getPlatformName()))
                        .doOnError(e -> log.error("Error during sync for platform {}: {}", adapter.getPlatformName(), e.getMessage(), e))
                        .onErrorResume(e -> Mono.empty()) // Changed Flux.empty() to Mono.empty()
                )
                .doOnComplete(() -> log.info("Finished scheduled synchronization for all platforms."))
                .subscribe(); // subscribe() is what triggers the execution
    }
}
