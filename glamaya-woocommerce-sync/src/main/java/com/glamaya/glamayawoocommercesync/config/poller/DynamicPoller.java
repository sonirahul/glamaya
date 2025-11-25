package com.glamaya.glamayawoocommercesync.config.poller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.util.DynamicPeriodicTrigger;

import java.time.Duration;

/**
 * Configuration class for defining dynamic poller metadata used in Spring Integration flows.
 * This allows for flexible and adaptive polling intervals.
 */
@Configuration
public class DynamicPoller {

    @Value("${application.processing.default-fetch-duration-ms:1000}")
    private int defaultFetchDurationMs;

    /**
     * Provides a prototype-scoped {@link PollerMetadata} bean.
     * This poller uses a {@link DynamicPeriodicTrigger} which allows its polling interval
     * to be changed at runtime, enabling adaptive polling strategies.
     *
     * @return A {@link PollerMetadata} instance configured with a dynamic trigger.
     */
    @Bean()
    @Scope("prototype") // Each integration flow should get its own instance
    public PollerMetadata poller() {
        DynamicPeriodicTrigger trigger = new DynamicPeriodicTrigger(
                Duration.ofMillis(defaultFetchDurationMs)); // Initial delay for the poller
        trigger.setFixedRate(true); // Ensure fixed-rate scheduling
        PollerMetadata pollerMetadata = new PollerMetadata();
        pollerMetadata.setTrigger(trigger);
        return pollerMetadata;
    }
}
