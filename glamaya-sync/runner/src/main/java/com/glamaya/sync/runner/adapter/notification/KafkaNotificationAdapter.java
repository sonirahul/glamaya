package com.glamaya.sync.runner.adapter.notification;

import com.glamaya.sync.core.domain.port.out.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Reactive Kafka implementation of the NotificationPort.
 * Dispatches canonical domain models to a Kafka topic.
 */
@Component
@ConditionalOnProperty(value = "glamaya.notifications.kafka.enabled", havingValue = "true")
public class KafkaNotificationAdapter implements NotificationPort<Object> {

    private static final Logger log = LoggerFactory.getLogger(KafkaNotificationAdapter.class);
    private final ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public KafkaNotificationAdapter(
            ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate,
            @Value("${glamaya.notifications.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public Mono<Void> notify(Object payload) {
        log.info("Sending payload to Kafka topic '{}': {}", topic, payload);
        return kafkaTemplate.send(topic, payload).then();
    }

    @Override
    public boolean supports(Object payload) {
        // This Kafka adapter supports all object types.
        return true;
    }
}
