package com.glamaya.sync.runner.config;

import com.glamaya.sync.core.application.service.SyncOrchestrationService;
import com.glamaya.sync.core.domain.model.EcomModel;
import com.glamaya.sync.core.domain.port.out.NotificationPort;
import com.glamaya.sync.core.domain.port.out.StatusStorePort;
import com.glamaya.sync.core.domain.port.out.SyncProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CoreWiringConfig {

    @Bean
    public SyncOrchestrationService syncOrchestrationService(StatusStorePort statusStorePort,
                                                             @Qualifier("compositeNotificationAdapter") NotificationPort<EcomModel<?>> notificationPort,
                                                             List<SyncProcessor<?, ?, ?>> syncProcessors) {
        return new SyncOrchestrationService(statusStorePort, notificationPort, syncProcessors);
    }
}
