package com.glamaya.glamayawoocommercesync.service.impl;

import com.glamaya.glamayawoocommercesync.config.kafka.KafkaProducer;
import com.glamaya.glamayawoocommercesync.port.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventPublisherKafkaImpl implements EventPublisher {
    private final KafkaProducer<Object> kafkaProducer;

    @Override
    public <K, V> void send(String topic, K key, V value) {
        kafkaProducer.send(topic, String.valueOf(key), value);
    }
}
