package com.glamaya.glamayawoocommercesync.config;

import com.glamaya.glamayawoocommercesync.domain.ProcessorType; // Updated import
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "application")
@Data
public class ApplicationProperties {
    private Processing processing;
    private Kafka kafka;
    private Map<String, ProcessorConfig> processors;

    // Manually added getters due to Lombok processing issues
    public Map<String, ProcessorConfig> getProcessors() {
        return processors;
    }

    public Processing getProcessing() {
        return processing;
    }

    public record Processing(int concurrency,
                             Backpressure backpressure,
                             RetryConfig retry,
                             BulkheadConfig bulkhead) { }

    public record Backpressure(int bufferSize, String overflowStrategy) { }
    public record RetryConfig(int maxAttempts,
                              int initialDelayMs,
                              int maxBackoffMs,
                              boolean enableCircuitBreaker,
                              int circuitBreakerFailureThreshold,
                              int circuitBreakerResetMs) { }
    public record BulkheadConfig(int limitRate, int maxInFlight) { }

    public record Kafka(Producer producer, Topics topic) { }
    public record Producer(int slowSendThresholdMs) { }
    public record Topics(String productEvents,
                         String userEvents,
                         String orderEvents,
                         String contactEvents,
                         String productEventsDlq,
                         String userEventsDlq,
                         String orderEventsDlq) { }

    public record ProcessorConfig(boolean enable,
                                  boolean resetOnStartup,
                                  int pageSize,
                                  FetchDurationMs fetchDurationMs,
                                  String queryUrl,
                                  String kafkaTopic,
                                  String contactKafkaTopic,
                                  N8n n8n,
                                  String sourceAccountName) { }

    public record FetchDurationMs(int active, int passive) { }
    public record N8n(boolean enable, String webhookUrl, String errorWebhookUrl) { }

    /**
     * Utility to get a processor config by ProcessorType or throw a clear exception if missing.
     *
     * @param processorType the ProcessorType enum (e.g., WOO_ORDER)
     * @return the ProcessorConfig for the given processor
     * @throws IllegalStateException if the config is missing
     */
    public ProcessorConfig getProcessorConfigOrThrow(ProcessorType processorType) {
        if (processorType == null) {
            throw new IllegalArgumentException("ProcessorType must not be null");
        }
        // Use enum name in lower case as the map key (e.g., "woo_order")
        String key = processorType.name().toLowerCase();
        ProcessorConfig config = this.getProcessors() != null ? this.getProcessors().get(key) : null;
        if (config == null) {
            throw new IllegalStateException("Missing configuration for processor '" + key + "' in application.properties");
        }
        return config;
    }

    @PostConstruct
    void validate() {
        if (processing == null) throw new IllegalStateException("application.processing section missing");
        if (processing.concurrency() <= 0) throw new IllegalStateException("application.processing.concurrency must be > 0");
        if (processing.backpressure() == null) throw new IllegalStateException("application.processing.backpressure missing");
        if (processing.backpressure().bufferSize() <= 0) throw new IllegalStateException("backpressure.buffer-size must be > 0");
        if (!StringUtils.hasText(processing.backpressure().overflowStrategy())) throw new IllegalStateException("backpressure.overflow-strategy must be set");
        if (processing.retry() == null) throw new IllegalStateException("application.processing.retry missing");
        if (processing.retry().maxAttempts() <= 0) throw new IllegalStateException("retry.max-attempts must be > 0");
        if (processing.retry().initialDelayMs() < 0) throw new IllegalStateException("retry.initial-delay-ms must be >= 0");
        if (processing.retry().maxBackoffMs() < processing.retry().initialDelayMs()) throw new IllegalStateException("retry.max-backoff-ms must be >= initial-delay-ms");
        if (processing.retry().enableCircuitBreaker()) {
            if (processing.retry().circuitBreakerFailureThreshold() <= 0) throw new IllegalStateException("retry.circuit-breaker-failure-threshold must be > 0 when circuit breaker enabled");
            if (processing.retry().circuitBreakerResetMs() <= 0) throw new IllegalStateException("retry.circuit-breaker-reset-ms must be > 0 when circuit breaker enabled");
        }
        if (processing.bulkhead() == null) throw new IllegalStateException("application.processing.bulkhead missing");
        if (processing.bulkhead().limitRate() <= 0) throw new IllegalStateException("bulkhead.limit-rate must be > 0");
        if (processing.bulkhead().maxInFlight() <= 0) throw new IllegalStateException("bulkhead.max-in-flight must be > 0");

        if (kafka == null || kafka.topic() == null) throw new IllegalStateException("application.kafka.topic section missing");
        // Validate required kafka topics
        if (!StringUtils.hasText(kafka.topic().productEvents())) throw new IllegalStateException("kafka.topic.product-events must be set");
        if (!StringUtils.hasText(kafka.topic().userEvents())) throw new IllegalStateException("kafka.topic.user-events must be set");
        if (!StringUtils.hasText(kafka.topic().orderEvents())) throw new IllegalStateException("kafka.topic.order-events must be set");
        if (!StringUtils.hasText(kafka.topic().contactEvents())) throw new IllegalStateException("kafka.topic.contact-events must be set");

        if (processors == null || processors.isEmpty()) throw new IllegalStateException("application.processors map must contain at least one processor config");
        processors.forEach((name, cfg) -> {
            if (cfg.pageSize() <= 0) throw new IllegalStateException("processor " + name + " page-size must be > 0");
            if (cfg.fetchDurationMs() == null) throw new IllegalStateException("processor " + name + " fetch-duration-ms missing");
            if (cfg.fetchDurationMs().active() <= 0) throw new IllegalStateException("processor " + name + " fetch-duration-ms.active must be > 0");
            if (!StringUtils.hasText(cfg.queryUrl())) throw new IllegalStateException("processor " + name + " query-url must be set");
            if (!StringUtils.hasText(cfg.kafkaTopic())) throw new IllegalStateException("processor " + name + " kafka-topic must be set");
            if (!StringUtils.hasText(cfg.contactKafkaTopic())) throw new IllegalStateException("processor " + name + " contact-kafka-topic must be set");
            if (!StringUtils.hasText(cfg.sourceAccountName())) throw new IllegalStateException("processor " + name + " source-account-name must be set");
        });
    }
}
