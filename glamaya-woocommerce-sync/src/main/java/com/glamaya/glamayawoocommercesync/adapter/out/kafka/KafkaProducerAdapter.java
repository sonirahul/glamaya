package com.glamaya.glamayawoocommercesync.adapter.out.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * An outbound adapter that wraps Spring Kafka's {@link KafkaTemplate} to send messages.
 * This class handles the low-level details of Kafka message production, including
 * adding custom headers and handling delivery acknowledgments and DLQ routing.
 *
 * @param <T> The type of the message payload.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducerAdapter<T> {

    @Value("${external.woocommerce.api.account-name}")
    private final String sourceAccountName;
    private final KafkaTemplate<String, T> kafkaTemplate;

    @Value("${application.kafka.dlq.enable:false}")
    private boolean dlqEnable;

    // DLQ topic names, configured via application properties
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

    /**
     * Sends a message to a specified Kafka topic.
     * Includes logic for logging send results, handling slow sends, and routing to a Dead Letter Queue (DLQ) on failure.
     *
     * @param topicName The name of the Kafka topic.
     * @param key       The message key.
     * @param message   The message payload.
     * @return A {@link CompletableFuture} that completes with the {@link SendResult} or an exception.
     */
    public CompletableFuture<SendResult<String, T>> send(String topicName, String key, T message) {

        long start = System.currentTimeMillis();
        var producerRecord = new ProducerRecord<>(topicName, key, message);
        // Add a custom header for source account name
        producerRecord.headers().add("__SourceAccountName__", sourceAccountName.getBytes());

        CompletableFuture<SendResult<String, T>> future = kafkaTemplate.send(producerRecord);

        // Add callback to log the delivery information and handle DLQ routing
        future.whenComplete((result, ex) -> {
            long duration = System.currentTimeMillis() - start;
            if (ex == null) {
                if (duration > slowSendThresholdMs) {
                    log.warn("Kafka Send: Slow send detected for topic={} key={} (duration={}ms)", topicName, key, duration);
                } else {
                    log.debug("Kafka Send: Success for topic={} key={} partition={} offset={} (duration={}ms)",
                            result.getRecordMetadata().topic(), key,
                            result.getRecordMetadata().partition(), result.getRecordMetadata().offset(), duration);
                }
            } else {
                log.error("Kafka Send: Failure for topic={} key={} (error={})", topicName, key, ex.toString());
                if (dlqEnable) {
                    String dlqTopic = resolveDlq(topicName);
                    if (dlqTopic != null) {
                        log.warn("Kafka DLQ: Routing failed message to DLQ topic={} (originalTopic={} key={})", dlqTopic, topicName, key);
                        kafkaTemplate.send(dlqTopic, key, message).whenComplete((dlqResult, dlqEx) -> {
                            if (dlqEx == null) {
                                log.info("Kafka DLQ: Send success to DLQ topic={} key={} partition={} offset={}", dlqResult.getRecordMetadata().topic(), key, dlqResult.getRecordMetadata().partition(), dlqResult.getRecordMetadata().offset());
                            } else {
                                log.error("Kafka DLQ: Send failure to DLQ topic={} key={} (error={})", dlqTopic, key, dlqEx.toString());
                            }
                        });
                    } else {
                        log.warn("Kafka DLQ: No specific DLQ topic found for {} and generic DLQ is not enabled for this topic.", topicName);
                    }
                } else {
                    log.warn("Kafka DLQ: DLQ is disabled for topic={}. Message not routed to DLQ.", topicName);
                }
            }
        });

        return future;
    }

    /**
     * Resolves the appropriate Dead Letter Queue (DLQ) topic for a given original topic.
     *
     * @param topicName The original Kafka topic name.
     * @return The DLQ topic name, or {@code null} if no specific DLQ is configured and generic DLQ is not applicable.
     */
    private String resolveDlq(String topicName) {
        // Prevent double-DLQ routing if the message is already in a DLQ
        if (topicName.equals(productEventsDlq.replace("-dlq", ""))) return productEventsDlq;
        if (topicName.equals(userEventsDlq.replace("-dlq", ""))) return userEventsDlq;
        if (topicName.equals(orderEventsDlq.replace("-dlq", ""))) return orderEventsDlq;
        if (topicName.equals(contactEventsDlq.replace("-dlq", ""))) return contactEventsDlq;

        // Fallback heuristic: append "-dlq" if not already a DLQ topic
        if (!topicName.endsWith("-dlq")) {
            return topicName + "-dlq";
        }
        return null;
    }

    /**
     * Flushes the Kafka producer, ensuring all buffered records are sent.
     */
    public void flush() {
        kafkaTemplate.flush();
    }
}
