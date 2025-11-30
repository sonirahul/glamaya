package com.glamaya.sync.runner.adapter.notification;

import com.glamaya.sync.core.domain.port.out.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A composite implementation of NotificationPort that dispatches notifications
 * to all other registered NotificationPort beans. This enables a "fan-out" mechanism.
 */
@Component
@Primary // Ensures this is the default implementation injected when NotificationPort is requested
public class CompositeNotificationAdapter implements NotificationPort<Object> {

    private static final Logger log = LoggerFactory.getLogger(CompositeNotificationAdapter.class);
    private final List<NotificationPort<Object>> notifiers;

    /**
     * Spring injects all available NotificationPort beans.
     * We filter out this instance to prevent infinite recursion.
     *
     * @param allNotificationPorts A list of all NotificationPort beans in the application context.
     */
    public CompositeNotificationAdapter(List<NotificationPort> allNotificationPorts) {
        this.notifiers = allNotificationPorts.stream()
                .filter(n -> n != this) // Exclude self to prevent infinite loop
                .map(n -> (NotificationPort<Object>) n) // Cast to generic type
                .collect(Collectors.toList());
        log.info("Initialized CompositeNotificationAdapter with {} leaf notifiers.", notifiers.size());
    }

    @Override
    public Mono<Void> notify(Object payload) {
        log.debug("CompositeNotificationAdapter dispatching payload to {} notifiers.", notifiers.size());
        return Flux.fromIterable(notifiers)
                .filter(notifier -> notifier.supports(payload))
                .flatMap(notifier -> notifier.notify(payload)
                        .doOnSuccess(v -> log.info("Payload dispatched by notifier: {}", notifier.getClass().getSimpleName()))
                        .doOnError(e -> log.error("Error dispatching payload via notifier {}: {}",
                                notifier.getClass().getSimpleName(), e.getMessage(), e))
                        .onErrorResume(e -> Mono.empty()) // Continue even if one notifier fails
                )
                .then();
    }

    @Override
    public boolean supports(Object payload) {
        // The composite adapter supports any payload that at least one of its delegates supports.
        return notifiers.stream().anyMatch(notifier -> notifier.supports(payload));
    }
}
