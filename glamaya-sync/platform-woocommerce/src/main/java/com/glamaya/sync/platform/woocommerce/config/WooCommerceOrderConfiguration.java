package com.glamaya.sync.platform.woocommerce.config;

import com.glamaya.sync.core.domain.model.ProcessorType;
import com.glamaya.sync.core.domain.port.out.ProcessorConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Loads WooCommerce Order related configuration from module-specific YAML and binds it to APIConfig.
 * Property prefix: glamaya.sync.woocommerce.api.orders-config
 */
@Configuration
@PropertySource(value = "classpath:application-woocommerce.yml", factory = YamlPropertySourceFactory.class)
@ConfigurationProperties(prefix = "glamaya.sync.woocommerce.api")
public class WooCommerceOrderConfiguration {

    /** Map bound from YAML: endpoint-configs (String key -> APIConfig). */
    private Map<String, APIConfig> endpointConfigs = new java.util.HashMap<>();

    public Map<String, APIConfig> getEndpointConfigs() { return endpointConfigs; }
    public void setEndpointConfigs(Map<String, APIConfig> endpointConfigs) { this.endpointConfigs = endpointConfigs; }

    /**
     * Expose a typed map keyed by ProcessorType, with processorType set in each APIConfig.
     */
    @Bean("wooEndpointConfigurations")
    public Map<ProcessorType, ProcessorConfiguration<APIConfig>> wooEndpointConfigurations() {
        Map<ProcessorType, ProcessorConfiguration<APIConfig>> result = endpointConfigs.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> ProcessorType.valueOf(e.getKey()),
                        e -> {
                            APIConfig cfg = e.getValue();
                            cfg.setProcessorType(ProcessorType.valueOf(e.getKey()));
                            return cfg; // APIConfig implements ProcessorConfiguration<APIConfig>
                        },
                        (a, b) -> a,
                        () -> new EnumMap<>(ProcessorType.class)
                ));
        return result;
    }
}
