package com.glamaya.glamayawoocommercesync.config;

import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "application")
@Data
public class ApplicationProperties {
    private Processing processing;
    private Kafka kafka;
    private Map<String, ProcessorConfig> processors;

    @Data
    public static class Processing {
        private int concurrency;
        private Backpressure backpressure;

        @Data
        public static class Backpressure {
            private int bufferSize;
            private String overflowStrategy;
        }
    }

    @Data
    public static class Kafka {
        private Producer producer;
        private Topics topic;

        @Data
        public static class Producer {
            private int slowSendThresholdMs;
        }

        @Data
        public static class Topics {
            private String productEvents;
            private String userEvents;
            private String orderEvents;
            private String contactEvents;
            private String productEventsDlq;
            private String userEventsDlq;
            private String orderEventsDlq;
        }
    }

    @Data
    public static class ProcessorConfig {
        private boolean enable;
        private boolean resetOnStartup;
        private int pageSize;
        private FetchDurationMs fetchDurationMs;
        private String queryUrl;
        private String kafkaTopic;
        private String contactKafkaTopic;
        private N8n n8n;
        private String sourceAccountName;
    }

    @Data
    public static class FetchDurationMs {
        private int active;
        private int passive;
    }

    @Data
    public static class N8n {
        private boolean enable;
        private String webhookUrl;
        private String errorWebhookUrl;
    }

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
}
