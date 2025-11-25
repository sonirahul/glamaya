package com.glamaya.glamayawoocommercesync.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * An event listener that captures {@link FetchCycleEvent}s and updates application metrics
 * using Micrometer. This class is responsible for providing insights into the performance
 * and behavior of the WooCommerce synchronization processors.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsEventListener {

    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> successCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> emptyCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> errorCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> processedCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> fetchTimers = new ConcurrentHashMap<>();

    /**
     * Listens for {@link FetchCycleEvent}s and records relevant metrics.
     * Metrics include fetch duration, success/empty/error counts, and processed item counts.
     *
     * @param evt The {@link FetchCycleEvent} to process.
     */
    @EventListener
    public void onFetchCycle(FetchCycleEvent evt) {
        String tag = evt.processorType().name();

        // Record fetch duration
        Timer timer = fetchTimers.computeIfAbsent(tag, t -> Timer.builder("woo_sync_fetch_duration")
                .tag("processor", t)
                .publishPercentileHistogram()
                .register(meterRegistry));
        timer.record(evt.durationNanos(), TimeUnit.NANOSECONDS);

        // Increment counters based on event type
        if (evt.error()) {
            increment(errorCounters, "woo_sync_fetch_error_total", tag);
            log.warn("Monitor: Fetch error for processor={} (duration={}ms)", tag, evt.durationNanos() / 1_000_000.0);
            return;
        }
        if (evt.empty()) {
            increment(emptyCounters, "woo_sync_fetch_empty_total", tag);
            log.debug("Monitor: Fetch empty for processor={} (duration={}ms)", tag, evt.durationNanos() / 1_000_000.0);
        } else {
            increment(successCounters, "woo_sync_fetch_success_total", tag);
            increment(processedCounters, "woo_sync_items_processed_total", tag, evt.itemCount());
            log.debug("Monitor: Fetch success for processor={} (items={}, duration={}ms)", tag, evt.itemCount(), evt.durationNanos() / 1_000_000.0);
        }
    }

    /**
     * Increments a counter metric by 1.0.
     *
     * @param map    The map holding the counters.
     * @param metric The base name of the metric.
     * @param tag    The processor tag for the metric.
     */
    private void increment(Map<String, Counter> map, String metric, String tag) {
        increment(map, metric, tag, 1.0);
    }

    /**
     * Increments a counter metric by a specified amount.
     *
     * @param map    The map holding the counters.
     * @param metric The base name of the metric.
     * @param tag    The processor tag for the metric.
     * @param amount The amount to increment the counter by.
     */
    private void increment(Map<String, Counter> map, String metric, String tag, double amount) {
        Counter c = map.computeIfAbsent(tag, t -> Counter.builder(metric).tag("processor", t).register(meterRegistry));
        c.increment(amount);
    }
}
