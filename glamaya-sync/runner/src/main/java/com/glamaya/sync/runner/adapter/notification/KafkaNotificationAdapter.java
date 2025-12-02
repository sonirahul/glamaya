package com.glamaya.sync.runner.adapter.notification;

import com.glamaya.sync.core.domain.model.NotificationType;
import com.glamaya.sync.core.domain.port.out.NotificationPort;
import com.glamaya.sync.core.domain.port.out.ProcessorConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Reactive Kafka implementation of the NotificationPort.
 * Dispatches canonical domain models to a Kafka topic.
 */
@Slf4j
@Component("kafkaNotificationAdapter")
public class KafkaNotificationAdapter implements NotificationPort<Object> {

    private final ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate;

    public KafkaNotificationAdapter(
            ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.KAFKA;
    }

    @Override
    public Mono<Void> notify(Object payload,
                             ProcessorConfiguration<?> processorConfiguration,
                             NotificationType type) {
        if (type != NotificationType.KAFKA) {
            return Mono.empty();
        }
        var cfg = processorConfiguration.getNotificationConfig(NotificationType.KAFKA);
        if (cfg == null || !Boolean.TRUE.equals(cfg.getEnable()) || cfg.getTopic() == null || cfg.getTopic().isBlank()) {
            return Mono.empty();
        }
        String topic = cfg.getTopic();
        log.debug("[KafkaNotificationAdapter] Sending payload to Kafka topic='{}'", topic);
        return kafkaTemplate.send(topic, payload).then();
    }
}
