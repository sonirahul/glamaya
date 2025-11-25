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

/**
 * Configures Spring Integration flows for synchronizing WooCommerce entities (Products, Users, Orders).
 * This class acts as a driving adapter, orchestrating the polling mechanism and
 * connecting it to the respective application services.
 */
@Configuration
@AllArgsConstructor
public class GlamWoocommerceIntegrationFlow {

    private final ApplicationProperties applicationProperties;

    /**
     * Defines the Spring Integration flow for Product synchronization.
     * It sets up a polling adapter that periodically triggers the ProductProcessor.
     *
     * @param productProcessor The application service responsible for processing products.
     * @param poller           The metadata for the polling schedule.
     * @return An {@link IntegrationFlow} for product synchronization.
     */
    @Bean
    public IntegrationFlow productFlow(ProductProcessor productProcessor, PollerMetadata poller) {
        // Create a polling adapter for the product processor, configured with properties.
        var adapter = new WooCommercePollingAdapter<>(
                productProcessor,
                poller,
                applicationProperties.getProcessorConfigOrThrow(productProcessor.getProcessorType()).enable(),
                applicationProperties.getProcessorConfigOrThrow(productProcessor.getProcessorType()).fetchDurationMs().active(),
                applicationProperties.getProcessorConfigOrThrow(productProcessor.getProcessorType()).fetchDurationMs().passive()
        );
        // Configure the integration flow: from the adapter (source), polled, and handled by the product processor.
        return IntegrationFlow.from(adapter, e -> e.poller(poller))
                .handle(productProcessor)
                .get();
    }

    /**
     * Defines the Spring Integration flow for User synchronization.
     * It sets up a polling adapter that periodically triggers the UserProcessor.
     *
     * @param userProcessor The application service responsible for processing users.
     * @param poller        The metadata for the polling schedule.
     * @return An {@link IntegrationFlow} for user synchronization.
     */
    @Bean
    public IntegrationFlow userFlow(UserProcessor userProcessor, PollerMetadata poller) {
        // Create a polling adapter for the user processor, configured with properties.
        var adapter = new WooCommercePollingAdapter<>(
                userProcessor,
                poller,
                applicationProperties.getProcessorConfigOrThrow(userProcessor.getProcessorType()).enable(),
                applicationProperties.getProcessorConfigOrThrow(userProcessor.getProcessorType()).fetchDurationMs().active(),
                applicationProperties.getProcessorConfigOrThrow(userProcessor.getProcessorType()).fetchDurationMs().passive()
        );
        // Configure the integration flow: from the adapter (source), polled, and handled by the user processor.
        return IntegrationFlow.from(adapter, e -> e.poller(poller))
                .handle(userProcessor)
                .get();
    }

    /**
     * Defines the Spring Integration flow for Order synchronization.
     * It sets up a polling adapter that periodically triggers the OrderProcessor.
     *
     * @param orderProcessor The application service responsible for processing orders.
     * @param poller         The metadata for the polling schedule.
     * @return An {@link IntegrationFlow} for order synchronization.
     */
    @Bean
    public IntegrationFlow orderFlow(OrderProcessor orderProcessor, PollerMetadata poller) {
        // Create a polling adapter for the order processor, configured with properties.
        var adapter = new WooCommercePollingAdapter<>(
                orderProcessor,
                poller,
                applicationProperties.getProcessorConfigOrThrow(orderProcessor.getProcessorType()).enable(),
                applicationProperties.getProcessorConfigOrThrow(orderProcessor.getProcessorType()).fetchDurationMs().active(),
                applicationProperties.getProcessorConfigOrThrow(orderProcessor.getProcessorType()).fetchDurationMs().passive()
        );
        // Configure the integration flow: from the adapter (source), polled, and handled by the order processor.
        return IntegrationFlow.from(adapter, e -> e.poller(poller))
                .handle(orderProcessor)
                .get();
    }
}
