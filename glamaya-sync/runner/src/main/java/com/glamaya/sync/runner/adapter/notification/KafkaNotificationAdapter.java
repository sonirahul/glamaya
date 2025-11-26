package com.glamaya.sync.runner.adapter.notification;

import com.glamaya.sync.core.domain.port.out.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka implementation of the NotificationPort.
 * Dispatches canonical domain models to a Kafka topic.
 */
@Component
@ConditionalOnProperty(value = "glamaya.notifications.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaNotificationAdapter implements NotificationPort<Object> {

    private static final Logger log = LoggerFactory.getLogger(KafkaNotificationAdapter.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic; // Assuming a single topic for simplicity, or can be dynamic

    public KafkaNotificationAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        // TODO: Make topic configurable, e.g., via @Value("${glamaya.notifications.kafka.topic}")
        this.topic = "glamaya-sync-events"; // Default topic
    }

    @Override
    public void notify(Object payload) {
        log.debug("Sending payload to Kafka topic '{}': {}", topic, payload);
        kafkaTemplate.send(topic, payload);
    }

    @Override
    public boolean supports(Object payload) {
        // For now, this Kafka adapter supports all object types.
        // In a more complex scenario, you might check payload type or content.
        return true;
    }
}
