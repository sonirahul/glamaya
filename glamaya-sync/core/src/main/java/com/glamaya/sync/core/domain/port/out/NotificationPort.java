package com.glamaya.sync.core.domain.port.out;

import com.glamaya.sync.core.domain.model.NotificationType;
import com.glamaya.sync.core.domain.model.EcomModel;
import reactor.core.publisher.Mono;

/**
 * Outbound notification port. Implementations send the payload to specific channels (Kafka, n8n, etc.).
 * The channel is indicated by NotificationType; per-channel config is read from ProcessorConfiguration.
 *
 * @param <C> payload type (must be an EcomModel)
 */
public interface NotificationPort<C extends EcomModel<?>> {

    /**
     * Returns whether this notifier supports the given channel type.
     */
    boolean supports(NotificationType type);

    /**
     * Notify the given channel.
     * Implementations should read per-channel config via processorConfiguration.getNotificationConfig(type).
     */
    Mono<Void> notify(C payload, ProcessorConfiguration<?> processorConfiguration, NotificationType type);
}
