package com.glamaya.sync.platform.woocommerce.config;

import com.glamaya.sync.core.domain.port.out.ProcessorConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

/**
 * Loads WooCommerce Order related configuration from module-specific YAML and binds it to APIConfig.
 * Property prefix: glamaya.sync.woocommerce.api.orders-config
 */
@Configuration
@PropertySource(value = "classpath:application-woocommerce.yml", factory = YamlPropertySourceFactory.class)
public class WooCommerceOrderConfiguration {

    @Bean("wooOrdersApiConfig")
    @ConfigurationProperties(prefix = "glamaya.sync.woocommerce.api.orders-config")
    public APIConfig wooOrdersApiConfig() {
        return new APIConfig();
    }

    @Bean("wooOrdersProcessorConfig")
    @Primary
    public ProcessorConfiguration<APIConfig> wooCommerceOrderProcessorConfiguration(@Qualifier("wooOrdersApiConfig") APIConfig apiConfig) {

        return new ProcessorConfiguration<APIConfig>() {
            private int currentPage = 0;

            @Override
            public APIConfig get() {
                return apiConfig;
            }

            @Override
            public boolean isEnable() {
                return apiConfig.isEnable();
            }

            @Override
            public boolean isResetOnStartup() {
                return apiConfig.isResetOnStartup();
            }

            @Override
            public int getPageSize() {
                return apiConfig.getPageSize();
            }

            @Override
            public int getCurrentPage() {
                return currentPage;
            }

            @Override
            public String getQueryUrl() {
                return apiConfig.getQueryUrl();
            }
        };
    }
}
