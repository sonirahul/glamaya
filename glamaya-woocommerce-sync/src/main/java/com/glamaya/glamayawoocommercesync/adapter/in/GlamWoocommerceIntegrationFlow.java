package com.glamaya.glamayawoocommercesync.adapter.in;

import com.glamaya.glamayawoocommercesync.application.service.OrderProcessor;
import com.glamaya.glamayawoocommercesync.application.service.ProductProcessor;
import com.glamaya.glamayawoocommercesync.application.service.UserProcessor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;

@Slf4j
@Configuration
@AllArgsConstructor
public class GlamWoocommerceIntegrationFlow {

    @Bean
    public IntegrationFlow productFlow(ProductProcessor productProcessor) {
        return IntegrationFlow.from(
                        productProcessor.receive(),
                        productProcessor.poll())
                .handle(productProcessor)
                .get();
    }

    @Bean
    public IntegrationFlow userFlow(UserProcessor userProcessor) {
        return IntegrationFlow.from(
                        userProcessor.receive(),
                        userProcessor.poll())
                .handle(userProcessor)
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
}
