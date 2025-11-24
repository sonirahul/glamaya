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

@Slf4j
public class WooCommercePollingAdapter<E> implements MessageSource<List<E>> {

    private final AbstractApplicationService<E> applicationService;
    private final PollerMetadata poller;
    private final boolean enable;
    private final int activeMillis; // New field
    private final int passiveMillis;
    private final ConcurrentLinkedQueue<List<E>> resultsQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean fetchInProgress = new AtomicBoolean(false);
    private final AtomicInteger consecutiveEmptyFetches = new AtomicInteger(0); // Renamed and moved
    private int disabledPollCount = 0;

    private static final int DISABLED_BASE_DELAY_MS = 1000;

    public WooCommercePollingAdapter(AbstractApplicationService<E> applicationService, PollerMetadata poller, boolean enable, int activeMillis, int passiveMillis) {
        this.applicationService = applicationService;
        this.poller = poller;
        this.enable = enable;
        this.activeMillis = activeMillis;
        this.passiveMillis = passiveMillis;
    }

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

    private void initiateFetchIfNotRunning() {
        if (fetchInProgress.compareAndSet(false, true)) {
            applicationService.fetchAndProcess()
                    .doFinally(sig -> fetchInProgress.set(false))
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

    private void handleEmptyFetchResult() {
        int emptyCount = consecutiveEmptyFetches.incrementAndGet();
        long backoff = Math.min((long) (activeMillis * Math.pow(2, emptyCount)), passiveMillis);
        log.info("No new entities found for processor={}. Empty fetches in a row: {}. Backing off for {}ms.", applicationService.getProcessorType(), emptyCount, backoff);
        modifyPollerDuration(poller, (int) backoff);
    }

    private void handleSuccessfulFetchResult() {
        consecutiveEmptyFetches.set(0);
        modifyPollerDuration(poller, activeMillis);
    }

    private boolean isProcessorEnabled() {
        if (!enable) {
            disabledPollCount++;
            long exponential = (long) (DISABLED_BASE_DELAY_MS * Math.pow(2, Math.min(disabledPollCount, 8)));
            long capped = Math.min(exponential, passiveMillis * 10L);
            long jitter = ThreadLocalRandom.current().nextLong(0, DISABLED_BASE_DELAY_MS);
            long nextDelay = capped + jitter;
            if (disabledPollCount % 10 == 1) {
                log.info("{} processor is disabled. Backing off for {}ms.", applicationService.getProcessorType(), nextDelay);
            }
            modifyPollerDuration(poller, (int) nextDelay);
            return false;
        }
        disabledPollCount = 0;
        return true;
    }

    private void modifyPollerDuration(PollerMetadata pollerMetadata, int millis) {
        if (pollerMetadata.getTrigger() instanceof DynamicPeriodicTrigger trigger) {
            trigger.setDuration(Duration.ofMillis(millis));
        }
    }
}
