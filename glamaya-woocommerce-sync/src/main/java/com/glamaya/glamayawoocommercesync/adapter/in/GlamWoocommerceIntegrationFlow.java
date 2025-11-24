package com.glamaya.glamayawoocommercesync.adapter.in;

import com.glamaya.glamayawoocommercesync.application.service.OrderProcessor;
import com.glamaya.glamayawoocommercesync.application.service.ProductProcessor;
import com.glamaya.glamayawoocommercesync.application.service.UserProcessor;
import com.glamaya.glamayawoocommercesync.config.ApplicationProperties;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.scheduling.PollerMetadata;

@Configuration
@AllArgsConstructor
public class GlamWoocommerceIntegrationFlow {

    private final ApplicationProperties applicationProperties;

    @Bean
    public IntegrationFlow productFlow(ProductProcessor productProcessor, PollerMetadata poller) {
        var adapter = new WooCommercePollingAdapter<>(
                productProcessor,
                poller,
                applicationProperties.getProcessorConfigOrThrow(productProcessor.getProcessorType()).enable(),
                applicationProperties.getProcessorConfigOrThrow(productProcessor.getProcessorType()).fetchDurationMs().active(), // Pass activeMillis
                applicationProperties.getProcessorConfigOrThrow(productProcessor.getProcessorType()).fetchDurationMs().passive()
        );
        return IntegrationFlow.from(adapter, e -> e.poller(poller))
                .handle(productProcessor)
                .get();
    }

    @Bean
    public IntegrationFlow userFlow(UserProcessor userProcessor, PollerMetadata poller) {
        var adapter = new WooCommercePollingAdapter<>(
                userProcessor,
                poller,
                applicationProperties.getProcessorConfigOrThrow(userProcessor.getProcessorType()).enable(),
                applicationProperties.getProcessorConfigOrThrow(userProcessor.getProcessorType()).fetchDurationMs().active(), // Pass activeMillis
                applicationProperties.getProcessorConfigOrThrow(userProcessor.getProcessorType()).fetchDurationMs().passive()
        );
        return IntegrationFlow.from(adapter, e -> e.poller(poller))
                .handle(userProcessor)
                .get();
    }

    @Bean
    public IntegrationFlow orderFlow(OrderProcessor orderProcessor, PollerMetadata poller) {
        var adapter = new WooCommercePollingAdapter<>(
                orderProcessor,
                poller,
                applicationProperties.getProcessorConfigOrThrow(orderProcessor.getProcessorType()).enable(),
                applicationProperties.getProcessorConfigOrThrow(orderProcessor.getProcessorType()).fetchDurationMs().active(), // Pass activeMillis
                applicationProperties.getProcessorConfigOrThrow(orderProcessor.getProcessorType()).fetchDurationMs().passive()
        );
        return IntegrationFlow.from(adapter, e -> e.poller(poller))
                .handle(orderProcessor)
                .get();
    }
}
