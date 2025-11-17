package com.glamaya.glamayawixsync.integration;

import com.glamaya.glamayawixsync.processor.CollectionProcessor;
import com.glamaya.glamayawixsync.processor.ContactProcessor;
import com.glamaya.glamayawixsync.processor.OrderProcessor;
import com.glamaya.glamayawixsync.processor.ProductProcessor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;

@Slf4j
@Configuration
@AllArgsConstructor
public class GlamWixIntegrationFlow {

    @Bean
    public IntegrationFlow productFlow(ProductProcessor productProcessor) {
        return IntegrationFlow.from(
                        productProcessor.receive(),
                        productProcessor.poll())
                .handle(productProcessor)
                .get();
    }

    @Bean
    public IntegrationFlow collectionFlow(CollectionProcessor collectionProcessor) {
        return IntegrationFlow.from(
                        collectionProcessor.receive(),
                        collectionProcessor.poll())
                .handle(collectionProcessor)
                .get();
    }

    @Bean
    public IntegrationFlow orderFlow(OrderProcessor orderProcessor) {
        return IntegrationFlow.from(
                        orderProcessor.receive(),
                        orderProcessor.poll())
                .handle(orderProcessor)
                .get();
    }

    @Bean
    public IntegrationFlow contactFlow(ContactProcessor contactProcessor) {
        return IntegrationFlow.from(
                        contactProcessor.receive(),
                        contactProcessor.poll())
                .handle(contactProcessor)
                .get();
    }
}