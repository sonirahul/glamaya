package com.glamaya.glamayawoocommercesync.adapter.out.kafka;

import com.glamaya.glamayawoocommercesync.port.out.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * An outbound adapter that implements the {@link EventPublisher} port using Kafka.
 * This class delegates event sending to a {@link KafkaProducerAdapter},
 * abstracting the Kafka-specific details from the application core.
 */
@Component
@RequiredArgsConstructor
public class EventPublisherKafkaAdapter implements EventPublisher {
    private final KafkaProducerAdapter<Object> kafkaProducer;

    /**
     * Sends an event to a specified Kafka topic.
     *
     * @param topic The name of the topic to send the event to.
     * @param key   The key for the event, used for partitioning.
     * @param value The value (payload) of the event.
     * @param <K>   The type of the event key.
     * @param <V>   The type of the event value.
     */
    @Override
    public <K, V> void send(String topic, K key, V value) {
        kafkaProducer.send(topic, String.valueOf(key), value);
    }
}
