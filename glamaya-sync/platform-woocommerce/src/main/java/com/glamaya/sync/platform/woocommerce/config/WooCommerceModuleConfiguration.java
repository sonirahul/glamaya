package com.glamaya.sync.platform.woocommerce.config;

import com.glamaya.datacontracts.ecommerce.mapper.ContactMapperFactory;
import com.glamaya.datacontracts.ecommerce.mapper.OrderMapperFactory;
import com.glamaya.datacontracts.ecommerce.mapper.WooOrderToContactMapperFactoryImpl;
import com.glamaya.datacontracts.ecommerce.mapper.WooOrderToOrderMapperFactoryImpl;
import com.glamaya.datacontracts.ecommerce.mapper.WooUserToContactMapperFactoryImpl;
import com.glamaya.datacontracts.woocommerce.Order;
import com.glamaya.datacontracts.woocommerce.User;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Spring configuration for the WooCommerce platform module.
 * This class defines beans for the WebClient and other necessary components
 * required for WooCommerce integration.
 */
@Configuration
public class WooCommerceModuleConfiguration {

    @Value("${glamaya.sync.woocommerce.api.baseUrl}")
    private String woocommerceApiBaseUrl;

    @Value("${glamaya.sync.woocommerce.api.max-in-memory-size}")
    private int maxInMemorySize;

    @Value("${glamaya.sync.woocommerce.api.response-timeout-in-millis}")
    private long responseTimeoutInMillis;

    /**
     * Configures and provides a WebClient instance for interacting with the WooCommerce API.
     * It's configured with an increased buffer size and a response timeout.
     * Authentication is handled dynamically per-request.
     *
     * @param builder WebClient.Builder provided by Spring.
     * @return Configured WebClient instance.
     */
    @Bean("woocommerceWebClient")
    public WebClient woocommerceWebClient(WebClient.Builder builder) {
        // Configure buffer size for large responses
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(maxInMemorySize))
                .build();

        // Configure timeouts on the underlying HTTP client
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 5 seconds connection timeout
                .responseTimeout(Duration.ofMillis(responseTimeoutInMillis))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(responseTimeoutInMillis, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS)));

        return builder
                .baseUrl(woocommerceApiBaseUrl)
                .exchangeStrategies(strategies)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * Provides the OrderMapperFactory implementation for WooCommerce orders.
     * This is required by the WooCommerceOrderDataMapper.
     *
     * @return An instance of WooOrderToOrderMapperFactoryImpl.
     */
    @Bean
    public OrderMapperFactory<Order> wooOrderToOrderMapperFactory() {
        return new WooOrderToOrderMapperFactoryImpl();
    }

    /**
     * Provides the ContactMapperFactory implementation for WooCommerce orders.
     * This is required by the WooCommerceUserDataMapper.
     *
     * @return An instance of WooUserToContactMapperFactoryImpl.
     */
    @Bean
    public ContactMapperFactory<User> wooUserToContactMapperFactory() {
        return new WooUserToContactMapperFactoryImpl();
    }

    /**
     * Provides the ContactMapperFactory implementation for WooCommerce orders to contact.
     * This is required by the WooCommerceOrderToContactDataMapper.
     *
     * @return An instance of WooUserToContactMapperFactoryImpl.
     */
    @Bean
    public ContactMapperFactory<Order> wooOrderToContactMapperFactoryImpl() {
        return new WooOrderToContactMapperFactoryImpl();
    }
}
