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

    @Value("${application.kafka.dlq.enable:false}")
    private boolean dlqEnable;

    @Value("${application.kafka.topic.product-events-dlq:woo-product-events-dlq}")
    private String productEventsDlq;
    @Value("${application.kafka.topic.user-events-dlq:woo-user-events-dlq}")
    private String userEventsDlq;
    @Value("${application.kafka.topic.order-events-dlq:woo-order-events-dlq}")
    private String orderEventsDlq;
    @Value("${application.kafka.topic.contact-events-dlq:ecommerce-contact-events-dlq}")
    private String contactEventsDlq;

    @Value("${application.kafka.producer.slow-send-threshold-ms:2000}")
    private long slowSendThresholdMs;

    public CompletableFuture<SendResult<String, T>> send(String topicName, String key, T message) {

        long start = System.currentTimeMillis();
        var producerRecord = new ProducerRecord<>(topicName, key, message);
        producerRecord.headers().add("__SourceAccountName__", sourceAccountName.getBytes());

        CompletableFuture<SendResult<String, T>> future = kafkaTemplate.send(producerRecord);

        // Add callback to log the delivery information
        future.whenComplete((result, ex) -> {
            long duration = System.currentTimeMillis() - start;
            if (ex == null) {
                if (duration > slowSendThresholdMs) {
                    log.warn("kafka_send slow topic={} key={} durationMs={}", topicName, key, duration);
                } else {
                    log.debug("kafka_send success topic={} key={} partition={} offset={} durationMs={}",
                            result.getRecordMetadata().topic(), key,
                            result.getRecordMetadata().partition(), result.getRecordMetadata().offset(), duration);
                }
            } else {
                log.error("kafka_send failure topic={} key={} error={}", topicName, key, ex.toString());
                if (dlqEnable) {
                    String dlqTopic = resolveDlq(topicName);
                    if (dlqTopic != null) {
                        log.warn("Routing failed message to DLQ dlqTopic={} originalTopic={} key={}", dlqTopic, topicName, key);
                        kafkaTemplate.send(dlqTopic, key, message).whenComplete((dlqResult, dlqEx) -> {
                            if (dlqEx == null) {
                                log.info("DLQ send success dlqTopic={} key={} partition={} offset={}", dlqResult.getRecordMetadata().topic(), key, dlqResult.getRecordMetadata().partition(), dlqResult.getRecordMetadata().offset());
                            } else {
                                log.error("DLQ send failure dlqTopic={} key={} error={}", dlqTopic, key, dlqEx.toString());
                            }
                        });
                    }
                }
            }
        });

        return future;
    }

    private String resolveDlq(String topicName) {
        if (topicName.equals(productEventsDlq.replace("-dlq", ""))) return productEventsDlq; // prevent double-dlq
        if (topicName.equals(userEventsDlq.replace("-dlq", ""))) return userEventsDlq;
        if (topicName.equals(orderEventsDlq.replace("-dlq", ""))) return orderEventsDlq;
        if (topicName.equals(contactEventsDlq.replace("-dlq", ""))) return contactEventsDlq;
        // fallback heuristic
        if (!topicName.endsWith("-dlq")) return topicName + "-dlq"; // generic suffix
        return null;
    }

    public void flush() {
        kafkaTemplate.flush();
    }
}