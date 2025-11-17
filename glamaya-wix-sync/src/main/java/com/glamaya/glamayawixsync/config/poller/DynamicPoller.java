package com.glamaya.glamayawixsync.config.poller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.util.DynamicPeriodicTrigger;

import java.time.Duration;

@Configuration
public class DynamicPoller {

    @Value("${application.wix.entities.global-fetch-duration-in-millis.init-mode}")
    private int globalFetchDurationInMillisInitMode;

    @Bean()
    @Scope("prototype")
    public PollerMetadata poller() {
        DynamicPeriodicTrigger trigger = new DynamicPeriodicTrigger(
                Duration.ofMillis(globalFetchDurationInMillisInitMode)); // Initial delay
        trigger.setFixedRate(true);
        PollerMetadata pollerMetadata = new PollerMetadata();
        pollerMetadata.setTrigger(trigger);
        return pollerMetadata;
    }
}
