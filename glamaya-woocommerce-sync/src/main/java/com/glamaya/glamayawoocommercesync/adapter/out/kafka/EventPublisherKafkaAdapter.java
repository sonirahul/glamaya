package com.glamaya.glamayawoocommercesync.adapter.out.kafka;

import com.glamaya.glamayawoocommercesync.port.out.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventPublisherKafkaAdapter implements EventPublisher {
    private final KafkaProducerAdapter<Object> kafkaProducer; // Updated type

    @Override
    public <K, V> void send(String topic, K key, V value) {
        kafkaProducer.send(topic, String.valueOf(key), value);
    }
}
