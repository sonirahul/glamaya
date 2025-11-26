package com.glamaya.sync.platform.woocommerce.config;

import com.glamaya.datacontracts.ecommerce.mapper.OrderMapperFactory;
import com.glamaya.datacontracts.ecommerce.mapper.WooOrderToOrderMapperFactoryImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Spring configuration for the WooCommerce platform module.
 * This class defines beans for the WebClient and other necessary components
 * required for WooCommerce integration.
 */
@Configuration
public class WooCommerceModuleConfiguration {

    @Value("${glamaya.sync.woocommerce.api.baseUrl}")
    private String woocommerceApiBaseUrl;

    @Value("${glamaya.sync.woocommerce.api.consumerKey}")
    private String woocommerceConsumerKey;

    @Value("${glamaya.sync.woocommerce.api.consumerSecret}")
    private String woocommerceConsumerSecret;

    /**
     * Configures and provides a WebClient instance for interacting with the WooCommerce API.
     * Includes basic authentication using consumer key and secret.
     *
     * @param builder WebClient.Builder provided by Spring.
     * @return Configured WebClient instance.
     */
    @Bean
    public WebClient woocommerceWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(woocommerceApiBaseUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(woocommerceConsumerKey, woocommerceConsumerSecret))
                .build();
    }

    /**
     * Provides the OrderMapperFactory implementation for WooCommerce orders.
     * This is required by the WooCommerceOrderDataMapper.
     *
     * @return An instance of WooOrderToOrderMapperFactoryImpl.
     */
    @Bean
    public OrderMapperFactory wooOrderToOrderMapperFactory() {
        return new WooOrderToOrderMapperFactoryImpl();
    }
}
