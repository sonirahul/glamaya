package com.glamaya.sync.runner.adapter.notification;

import com.glamaya.sync.core.domain.model.NotificationType;
import com.glamaya.sync.core.domain.port.out.NotificationPort;
import com.glamaya.sync.core.domain.port.out.ProcessorConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * A composite implementation of NotificationPort that dispatches notifications
 * to all other registered NotificationPort beans. This enables a "fan-out" mechanism.
 */
@Slf4j
@Component("compositeNotificationAdapter")
@Primary // Ensures this is the default implementation injected when NotificationPort is requested
public class CompositeNotificationAdapter implements NotificationPort<Object> {

    private final List<NotificationPort<Object>> notifiers;

    /**
     * Spring injects all available NotificationPort beans.
     * We filter out this instance to prevent infinite recursion.
     *
     * @param allNotificationPorts A list of all NotificationPort beans in the application context.
     */
    public CompositeNotificationAdapter(List<NotificationPort<Object>> allNotificationPorts) {
        this.notifiers = allNotificationPorts.stream()
                .filter(n -> n != this) // Exclude self to prevent infinite loop
                .toList();
        log.info("Initialized CompositeNotificationAdapter with {} leaf notifiers.", notifiers.size());
    }

    @Override
    public boolean supports(NotificationType type) {
        return true; // composite delegates to leaf supports
    }

    @Override
    public Mono<Void> notify(Object payload,
                             ProcessorConfiguration<?> processorConfiguration,
                             NotificationType type) {
        // Retrieve channel-specific config once; if absent, skip.
        var channelCfg = processorConfiguration.getNotificationConfig(type);
        if (channelCfg == null) {
            return Mono.empty();
        }
        return Flux.fromIterable(notifiers)
                .filter(n -> n.supports(type))
                .flatMap(n -> n.notify(payload, processorConfiguration, type)
                        .onErrorResume(e -> Mono.empty()))
                .then();
    }
}
