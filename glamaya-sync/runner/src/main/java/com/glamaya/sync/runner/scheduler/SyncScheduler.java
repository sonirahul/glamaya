package com.glamaya.sync.runner.scheduler;

import com.glamaya.sync.core.domain.port.out.PlatformAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
     * Runs every 5 minutes (300000 ms) after the previous task completes.
     * The fixedDelay ensures that there's a 5-minute gap between the end of one execution
     * and the start of the next, preventing overlapping executions.
     */
    @Scheduled(fixedDelayString = "${glamaya.sync.scheduler.fixedDelay:300000}") // Default to 5 minutes
    public void runAllPlatformSyncs() {
        log.info("Starting scheduled synchronization for all platforms.");
        for (PlatformAdapter adapter : platformAdapters) {
            try {
                log.info("Running sync for platform: {}", adapter.getPlatformName());
                adapter.sync();
                log.info("Sync completed for platform: {}", adapter.getPlatformName());
            } catch (Exception e) {
                log.error("Error during sync for platform {}: {}", adapter.getPlatformName(), e.getMessage(), e);
            }
        }
        log.info("Finished scheduled synchronization for all platforms.");
    }
}
