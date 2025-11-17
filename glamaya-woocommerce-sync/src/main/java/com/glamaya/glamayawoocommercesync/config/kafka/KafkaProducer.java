package com.glamaya.glamayawoocommercesync.config.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer<T> {

    @Value("${external.woocommerce.api.account-name}")
    private final String sourceAccountName;
    private final KafkaTemplate<String, T> kafkaTemplate;

    public CompletableFuture<SendResult<String, T>> send(String topicName, String key, T message) {

        var producerRecord = new ProducerRecord<>(topicName, key, message);
        producerRecord.headers().add("__SourceAccountName__", sourceAccountName.getBytes());

        CompletableFuture<SendResult<String, T>> future = kafkaTemplate.send(producerRecord);

        // Add callback to log the delivery information
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Message sent successfully to topic: {}, key: {}, partition: {}, offset: {}",
                        result.getRecordMetadata().topic(),
                        key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send message to topic: {}, key: {}", topicName, key, ex);
            }
        });

        return future;
    }

    public void flush() {
        kafkaTemplate.flush();
    }
}