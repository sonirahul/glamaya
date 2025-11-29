package com.glamaya.sync.platform.woocommerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
}
