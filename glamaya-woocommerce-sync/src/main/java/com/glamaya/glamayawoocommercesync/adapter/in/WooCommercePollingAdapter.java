package com.glamaya.glamayawoocommercesync.adapter.in;

import com.glamaya.glamayawoocommercesync.application.service.AbstractApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.util.DynamicPeriodicTrigger;
import org.springframework.messaging.Message;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Spring Integration {@link MessageSource} adapter that drives the WooCommerce synchronization process.
 * This adapter is responsible for:
 * <ul>
 *     <li>Managing the polling schedule and adaptive backoff based on fetch results.</li>
 *     <li>Initiating fetch operations by calling the {@link AbstractApplicationService}.</li>
 *     <li>Queuing fetched entities and delivering them in batches to the downstream handler.</li>
 *     <li>Handling disabled processor states and adjusting polling frequency.</li>
 * </ul>
 * This class acts as a driving adapter in the hexagonal architecture, decoupling the application's core
 * logic from the specific polling mechanism.
 *
 * @param <E> The type of the WooCommerce entity being processed.
 */
@Slf4j
public class WooCommercePollingAdapter<E> implements MessageSource<List<E>> {

    private final AbstractApplicationService<E> applicationService;
    private final PollerMetadata poller;
    private final boolean enable;
    private final int activeMillis;
    private final int passiveMillis;

    // State management for polling and backoff
    private final ConcurrentLinkedQueue<List<E>> resultsQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean fetchInProgress = new AtomicBoolean(false);
    private final AtomicInteger consecutiveEmptyFetches = new AtomicInteger(0);
    private int disabledPollCount = 0;

    private static final int DISABLED_BASE_DELAY_MS = 1000;

    /**
     * Constructs a new {@code WooCommercePollingAdapter}.
     *
     * @param applicationService The application service to be driven by this adapter.
     * @param poller             The {@link PollerMetadata} to control the polling schedule.
     * @param enable             Whether this processor is enabled.
     * @param activeMillis       The polling interval when new data is consistently fetched.
     * @param passiveMillis      The polling interval when no new data is found (maximum backoff).
     */
    public WooCommercePollingAdapter(AbstractApplicationService<E> applicationService, PollerMetadata poller, boolean enable, int activeMillis, int passiveMillis) {
        this.applicationService = applicationService;
        this.poller = poller;
        this.enable = enable;
        this.activeMillis = activeMillis;
        this.passiveMillis = passiveMillis;
    }

    /**
     * Receives messages (batches of entities) for the Spring Integration flow.
     * This method is called by the poller to check for new data.
     *
     * @return A {@link Message} containing a list of entities, or {@code null} if no data is ready.
     */
    @Override
    public Message<List<E>> receive() {
        if (!isProcessorEnabled()) {
            return null;
        }

        List<E> readyBatch = resultsQueue.poll();
        if (readyBatch != null) {
            log.debug("Delivering batch of {} entities for processor={}", readyBatch.size(), applicationService.getProcessorType());
            return MessageBuilder.withPayload(readyBatch).build();
        }

        initiateFetchIfNotRunning();
        return null;
    }

    /**
     * Initiates an asynchronous fetch operation if one is not already in progress.
     * It calls the application service to perform the core fetch-and-process logic.
     */
    private void initiateFetchIfNotRunning() {
        // Ensure only one fetch operation runs at a time.
        if (fetchInProgress.compareAndSet(false, true)) {
            applicationService.fetchAndProcess()
                    .doFinally(sig -> fetchInProgress.set(false)) // Always reset flag when Mono terminates
                    .subscribe(
                            isEmptyFetch -> {
                                if (isEmptyFetch) {
                                    handleEmptyFetchResult();
                                } else {
                                    handleSuccessfulFetchResult();
                                }
                            },
                            error -> log.error("Error in polling adapter for processor={}", applicationService.getProcessorType(), error)
                    );
        }
    }

    /**
     * Handles the scenario when the {@link AbstractApplicationService} reports an empty fetch result.
     * This triggers an exponential backoff for the polling duration.
     */
    private void handleEmptyFetchResult() {
        int emptyCount = consecutiveEmptyFetches.incrementAndGet();
        // Calculate exponential backoff, capped by passiveMillis
        long backoff = Math.min((long) (activeMillis * Math.pow(2, emptyCount)), passiveMillis);
        log.info("No new entities found for processor={}. Empty fetches in a row: {}. Backing off for {}ms.", applicationService.getProcessorType(), emptyCount, backoff);
        modifyPollerDuration(poller, (int) backoff);
    }

    /**
     * Handles the scenario when the {@link AbstractApplicationService} reports a successful (non-empty) fetch result.
     * This resets the empty fetch counter and sets the polling duration back to {@code activeMillis}.
     */
    private void handleSuccessfulFetchResult() {
        consecutiveEmptyFetches.set(0); // Reset counter on successful fetch
        modifyPollerDuration(poller, activeMillis);
    }

    /**
     * Checks if the processor is currently enabled. If disabled, it applies an exponential backoff
     * to the poller duration to reduce unnecessary polling attempts.
     *
     * @return {@code true} if the processor is enabled, {@code false} otherwise.
     */
    private boolean isProcessorEnabled() {
        if (!enable) {
            disabledPollCount++;
            // Calculate exponential backoff for disabled state
            long exponential = (long) (DISABLED_BASE_DELAY_MS * Math.pow(2, Math.min(disabledPollCount, 8)));
            long capped = Math.min(exponential, passiveMillis * 10L); // Cap at 10x passive
            long jitter = ThreadLocalRandom.current().nextLong(0, DISABLED_BASE_DELAY_MS); // Add jitter
            long nextDelay = capped + jitter;
            if (disabledPollCount % 10 == 1) { // Log less frequently
                log.info("{} processor is disabled. Backing off for {}ms.", applicationService.getProcessorType(), nextDelay);
            }
            modifyPollerDuration(poller, (int) nextDelay);
            return false;
        }
        disabledPollCount = 0; // Reset disabled counter if enabled
        return true;
    }

    /**
     * Dynamically updates the duration of the poller's trigger.
     *
     * @param pollerMetadata The {@link PollerMetadata} whose trigger needs modification.
     * @param millis         The new duration in milliseconds.
     */
    private void modifyPollerDuration(PollerMetadata pollerMetadata, int millis) {
        if (pollerMetadata.getTrigger() instanceof DynamicPeriodicTrigger trigger) {
            trigger.setDuration(Duration.ofMillis(millis));
        }
    }
}
