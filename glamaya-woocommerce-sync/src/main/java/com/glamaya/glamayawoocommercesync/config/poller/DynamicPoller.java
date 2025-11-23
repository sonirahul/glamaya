package com.glamaya.glamayawoocommercesync.config.poller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.util.DynamicPeriodicTrigger;

import java.time.Duration;

@Configuration
public class DynamicPoller {

    @Value("${application.processing.default-fetch-duration-ms:1000}")
    private int defaultFetchDurationMs;

    @Bean()
    @Scope("prototype")
    public PollerMetadata poller() {
        DynamicPeriodicTrigger trigger = new DynamicPeriodicTrigger(
                Duration.ofMillis(defaultFetchDurationMs)); // Initial delay
        trigger.setFixedRate(true);
        PollerMetadata pollerMetadata = new PollerMetadata();
        pollerMetadata.setTrigger(trigger);
        return pollerMetadata;
    }
}
