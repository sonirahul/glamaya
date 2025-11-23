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

    @EventListener
    public void onFetchCycle(FetchCycleEvent evt) {
        String tag = evt.processorType().name();
        Timer timer = fetchTimers.computeIfAbsent(tag, t -> Timer.builder("woo_sync_fetch_duration")
                .tag("processor", t)
                .publishPercentileHistogram()
                .register(meterRegistry));
        timer.record(evt.durationNanos(), TimeUnit.NANOSECONDS);

        if (evt.error()) {
            increment(errorCounters, "woo_sync_fetch_error_total", tag);
            log.warn("monitor fetch error processor={} durationMs={}", tag, evt.durationNanos() / 1_000_000.0);
            return;
        }
        if (evt.empty()) {
            increment(emptyCounters, "woo_sync_fetch_empty_total", tag);
            log.debug("monitor fetch empty processor={} durationMs={}", tag, evt.durationNanos() / 1_000_000.0);
        } else {
            increment(successCounters, "woo_sync_fetch_success_total", tag);
            increment(processedCounters, "woo_sync_items_processed_total", tag, evt.itemCount());
            log.debug("monitor fetch success processor={} items={} durationMs={}", tag, evt.itemCount(), evt.durationNanos() / 1_000_000.0);
        }
    }

    private void increment(Map<String, Counter> map, String metric, String tag) {
        increment(map, metric, tag, 1.0);
    }

    private void increment(Map<String, Counter> map, String metric, String tag, double amount) {
        Counter c = map.computeIfAbsent(tag, t -> Counter.builder(metric).tag("processor", t).register(meterRegistry));
        c.increment(amount);
    }
}
