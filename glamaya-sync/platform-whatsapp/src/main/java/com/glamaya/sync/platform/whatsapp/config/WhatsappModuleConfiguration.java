package com.glamaya.sync.platform.whatsapp.config;

import com.glamaya.datacontracts.ecommerce.mapper.ContactMapperFactory;
import com.glamaya.datacontracts.ecommerce.mapper.WhatsappToContactMapperFactoryImpl;
import com.glamaya.datacontracts.whatsapp.Chat;
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
 * Spring configuration for the WhatsApp platform module.
 * This class defines beans for the WebClient and other necessary components
 * required for WhatsApp integration.
 */
@Configuration
public class WhatsappModuleConfiguration {

    @Value("${glamaya.sync.whatsapp.api.baseUrl}")
    private String whatsappApiBaseUrl;

    @Value("${glamaya.sync.whatsapp.api.max-in-memory-size}")
    private int maxInMemorySize;

    @Value("${glamaya.sync.whatsapp.api.response-timeout-in-millis}")
    private long responseTimeoutInMillis;

    /**
     * Configures and provides a WebClient instance for interacting with the WhatsApp API.
     * It's configured with an increased buffer size and a response timeout.
     * Authentication is handled dynamically per-request.
     *
     * @param builder WebClient.Builder provided by Spring.
     * @return Configured WebClient instance.
     */
    @Bean("whatsappWebClient")
    public WebClient whatsappWebClient(WebClient.Builder builder) {
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
                .baseUrl(whatsappApiBaseUrl)
                .exchangeStrategies(strategies)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * Provides the ContactMapperFactory implementation for WhatsApp chats.
     * This is required by the WhatsappUserDataMapper.
     *
     * @return An instance of WhatsappToContactMapperFactoryImpl.
     */
    @Bean
    public ContactMapperFactory<Chat> whatsappToContactMapperFactoryImpl() {
        return new WhatsappToContactMapperFactoryImpl();
    }

}
