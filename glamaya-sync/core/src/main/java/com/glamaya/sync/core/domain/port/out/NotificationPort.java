package com.glamaya.sync.core.domain.port.out;

import reactor.core.publisher.Mono;

/**
 * A generic outbound port for dispatching a canonical domain model to an external system.
 * This could be a message broker, a webhook, an email service, etc.
 *
 * @param <C> The type of the canonical domain model (the payload).
 */
public interface NotificationPort<C> {

    /**
     * Notifies an external system of an event.
     *
     * @param payload The canonical domain object.
     * @return A Mono<Void> that completes when the notification has been sent.
     */
    Mono<Void> notify(C payload);

    /**
     * A method to check if this notifier is configured to handle a specific type of payload.
     * This allows for selective notification.
     *
     * @param payload The payload to check.
     * @return true if this notifier should process this payload, false otherwise.
     */
    boolean supports(C payload);
}
