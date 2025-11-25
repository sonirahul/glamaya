package com.glamaya.glamayawoocommercesync.config;

import com.glamaya.glamayawoocommercesync.domain.ProcessorType;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Configuration properties for the WooCommerce synchronization application.
 * Binds properties from application.properties (or similar sources) to Java objects,
 * providing structured access to various application settings.
 */
@Component
@ConfigurationProperties(prefix = "application")
@Data
public class ApplicationProperties {
    private Processing processing;
    private Kafka kafka;
    private Map<String, ProcessorConfig> processors;

    // Manually added getters due to Lombok processing issues in some environments
    public Map<String, ProcessorConfig> getProcessors() {
        return processors;
    }

    public Processing getProcessing() {
        return processing;
    }

    /**
     * Configuration related to general processing behavior.
     *
     * @param concurrency  The maximum number of concurrent entity processing operations.
     * @param backpressure Backpressure configuration.
     * @param retry        Retry policy configuration.
     * @param bulkhead     Bulkhead configuration.
     */
    public record Processing(int concurrency,
                             Backpressure backpressure,
                             RetryConfig retry,
                             BulkheadConfig bulkhead) {
    }

    /**
     * Configuration for backpressure handling.
     *
     * @param bufferSize       The size of the buffer for backpressure.
     * @param overflowStrategy The strategy to use when the buffer overflows.
     */
    public record Backpressure(int bufferSize, String overflowStrategy) {
    }

    /**
     * Configuration for retry mechanisms and circuit breaker.
     *
     * @param maxAttempts                    The maximum number of retry attempts.
     * @param initialDelayMs                 The initial delay in milliseconds before the first retry.
     * @param maxBackoffMs                   The maximum backoff delay in milliseconds between retries.
     * @param enableCircuitBreaker           Whether to enable the circuit breaker pattern.
     * @param circuitBreakerFailureThreshold The number of consecutive failures to open the circuit.
     * @param circuitBreakerResetMs          The time in milliseconds after which a tripped circuit attempts to close.
     */
    public record RetryConfig(int maxAttempts,
                              int initialDelayMs,
                              int maxBackoffMs,
                              boolean enableCircuitBreaker,
                              int circuitBreakerFailureThreshold,
                              int circuitBreakerResetMs) {
    }

    /**
     * Configuration for bulkhead pattern to limit concurrent access.
     *
     * @param limitRate   The maximum rate of requests.
     * @param maxInFlight The maximum number of in-flight requests.
     */
    public record BulkheadConfig(int limitRate, int maxInFlight) {
    }

    /**
     * Configuration related to Kafka messaging.
     *
     * @param producer Producer-specific settings.
     * @param topic    Topic names configuration.
     */
    public record Kafka(Producer producer, Topics topic) {
    }

    /**
     * Kafka producer specific settings.
     *
     * @param slowSendThresholdMs Threshold in milliseconds to log a Kafka send as slow.
     */
    public record Producer(int slowSendThresholdMs) {
    }

    /**
     * Kafka topic names used by the application.
     *
     * @param productEvents    Topic for product events.
     * @param userEvents       Topic for user events.
     * @param orderEvents      Topic for order events.
     * @param contactEvents    Topic for contact events.
     * @param productEventsDlq Dead Letter Queue topic for product events.
     * @param userEventsDlq    Dead Letter Queue topic for user events.
     * @param orderEventsDlq   Dead Letter Queue topic for order events.
     */
    public record Topics(String productEvents,
                         String userEvents,
                         String orderEvents,
                         String contactEvents,
                         String productEventsDlq,
                         String userEventsDlq,
                         String orderEventsDlq) {
    }

    /**
     * Configuration specific to a single processor type.
     *
     * @param enable            Whether this processor is enabled.
     * @param resetOnStartup    Whether to reset the tracker on application startup.
     * @param pageSize          The number of entities to fetch per page.
     * @param fetchDurationMs   Fetch duration settings (active/passive).
     * @param queryUrl          The WooCommerce API query URL for this processor.
     * @param kafkaTopic        The Kafka topic for primary events from this processor.
     * @param contactKafkaTopic The Kafka topic for contact events from this processor.
     * @param n8n               n8n notification settings.
     * @param sourceAccountName The source account name for events.
     */
    public record ProcessorConfig(boolean enable,
                                  boolean resetOnStartup,
                                  int pageSize,
                                  FetchDurationMs fetchDurationMs,
                                  String queryUrl,
                                  String kafkaTopic,
                                  String contactKafkaTopic,
                                  N8n n8n,
                                  String sourceAccountName) {
    }

    /**
     * Fetch duration settings for a processor.
     *
     * @param active  The active polling interval in milliseconds when data is being fetched.
     * @param passive The passive polling interval in milliseconds when no new data is found.
     */
    public record FetchDurationMs(int active, int passive) {
    }

    /**
     * n8n notification settings for a processor.
     *
     * @param enable          Whether n8n notifications are enabled.
     * @param webhookUrl      The n8n webhook URL for success notifications.
     * @param errorWebhookUrl The n8n webhook URL for error notifications.
     */
    public record N8n(boolean enable, String webhookUrl, String errorWebhookUrl) {
    }

    /**
     * Utility to get a processor configuration by {@link ProcessorType} or throw a clear exception if missing.
     *
     * @param processorType The {@link ProcessorType} enum (e.g., {@code WOO_ORDER}).
     * @return The {@link ProcessorConfig} for the given processor.
     * @throws IllegalStateException    if the configuration is missing.
     * @throws IllegalArgumentException if {@code processorType} is null.
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

    /**
     * Validates the loaded application properties after construction.
     * Throws {@link IllegalStateException} if any required property is missing or invalid.
     */
    @PostConstruct
    void validate() {
        if (processing == null) throw new IllegalStateException("application.processing section missing");
        if (processing.concurrency() <= 0)
            throw new IllegalStateException("application.processing.concurrency must be > 0");
        if (processing.backpressure() == null)
            throw new IllegalStateException("application.processing.backpressure missing");
        if (processing.backpressure().bufferSize() <= 0)
            throw new IllegalStateException("backpressure.buffer-size must be > 0");
        if (!StringUtils.hasText(processing.backpressure().overflowStrategy()))
            throw new IllegalStateException("backpressure.overflow-strategy must be set");
        if (processing.retry() == null) throw new IllegalStateException("application.processing.retry missing");
        if (processing.retry().maxAttempts() <= 0) throw new IllegalStateException("retry.max-attempts must be > 0");
        if (processing.retry().initialDelayMs() < 0)
            throw new IllegalStateException("retry.initial-delay-ms must be >= 0");
        if (processing.retry().maxBackoffMs() < processing.retry().initialDelayMs())
            throw new IllegalStateException("retry.max-backoff-ms must be >= initial-delay-ms");
        if (processing.retry().enableCircuitBreaker()) {
            if (processing.retry().circuitBreakerFailureThreshold() <= 0)
                throw new IllegalStateException("retry.circuit-breaker-failure-threshold must be > 0 when circuit breaker enabled");
            if (processing.retry().circuitBreakerResetMs() <= 0)
                throw new IllegalStateException("retry.circuit-breaker-reset-ms must be > 0 when circuit breaker enabled");
        }
        if (processing.bulkhead() == null) throw new IllegalStateException("application.processing.bulkhead missing");
        if (processing.bulkhead().limitRate() <= 0) throw new IllegalStateException("bulkhead.limit-rate must be > 0");
        if (processing.bulkhead().maxInFlight() <= 0)
            throw new IllegalStateException("bulkhead.max-in-flight must be > 0");

        if (kafka == null || kafka.topic() == null)
            throw new IllegalStateException("application.kafka.topic section missing");
        // Validate required kafka topics
        if (!StringUtils.hasText(kafka.topic().productEvents()))
            throw new IllegalStateException("kafka.topic.product-events must be set");
        if (!StringUtils.hasText(kafka.topic().userEvents()))
            throw new IllegalStateException("kafka.topic.user-events must be set");
        if (!StringUtils.hasText(kafka.topic().orderEvents()))
            throw new IllegalStateException("kafka.topic.order-events must be set");
        if (!StringUtils.hasText(kafka.topic().contactEvents()))
            throw new IllegalStateException("kafka.topic.contact-events must be set");

        if (processors == null || processors.isEmpty())
            throw new IllegalStateException("application.processors map must contain at least one processor config");
        processors.forEach((name, cfg) -> {
            if (cfg.pageSize() <= 0) throw new IllegalStateException("processor " + name + " page-size must be > 0");
            if (cfg.fetchDurationMs() == null)
                throw new IllegalStateException("processor " + name + " fetch-duration-ms missing");
            if (cfg.fetchDurationMs().active() <= 0)
                throw new IllegalStateException("processor " + name + " fetch-duration-ms.active must be > 0");
            if (cfg.fetchDurationMs().passive() <= 0)
                throw new IllegalStateException("processor " + name + " fetch-duration-ms.passive must be > 0");
            if (!StringUtils.hasText(cfg.queryUrl()))
                throw new IllegalStateException("processor " + name + " query-url must be set");
            if (!StringUtils.hasText(cfg.kafkaTopic()))
                throw new IllegalStateException("processor " + name + " kafka-topic must be set");
            if (!StringUtils.hasText(cfg.contactKafkaTopic()))
                throw new IllegalStateException("processor " + name + " contact-kafka-topic must be set");
            if (!StringUtils.hasText(cfg.sourceAccountName()))
                throw new IllegalStateException("processor " + name + " source-account-name must be set");
        });
    }
}
