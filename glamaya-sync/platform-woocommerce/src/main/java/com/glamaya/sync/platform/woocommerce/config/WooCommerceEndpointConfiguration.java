package com.glamaya.sync.platform.woocommerce.config;

import com.glamaya.sync.core.domain.model.NotificationType;
import com.glamaya.sync.core.domain.model.ProcessorType;
import com.glamaya.sync.core.domain.port.out.ProcessorConfiguration;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Central WooCommerce endpoint configuration loader.
 * Binds dynamic endpoint-configs from application-woocommerce.yml keyed by ProcessorType enum name.
 * Provides defensive copies via getConfiguration(...) to prevent mutation of internal state.
 */
@Configuration
@PropertySource(value = "classpath:application-woocommerce.yml", factory = YamlPropertySourceFactory.class)
@ConfigurationProperties(prefix = "glamaya.sync.woocommerce.api")
public class WooCommerceEndpointConfiguration {

    /**
     * YAML: endpoint-configs (String key -> APIConfig).
     */
    private Map<String, APIConfig> endpointConfigs = new HashMap<>();

    void setEndpointConfigs(Map<String, APIConfig> endpointConfigs) {
        this.endpointConfigs = endpointConfigs;
    }

    @PostConstruct
    void validateAndNormalize() {
        if (endpointConfigs == null || endpointConfigs.isEmpty()) {
            throw new IllegalStateException("No endpoint-configs defined under glamaya.sync.woocommerce.api.endpoint-configs");
        }
        endpointConfigs.forEach((key, cfg) -> {
            if (cfg == null) {
                throw new IllegalStateException("Null config block for key: " + key);
            }
            if (cfg.getQueryUrl() == null || cfg.getQueryUrl().isBlank()) {
                throw new IllegalStateException("Missing query-url for endpoint-config: " + key);
            }
            if (cfg.getPageSize() == null || cfg.getPageSize() <= 0) {
                throw new IllegalStateException("Invalid page-size (null or <=0) for endpoint-config: " + key);
            }
            if (cfg.getInitPage() == null || cfg.getInitPage() <= 0) {
                throw new IllegalStateException("Invalid init-page (null or <=0) for endpoint-config: " + key);
            }
        });
    }

    public ProcessorConfiguration<APIConfig> getConfiguration(ProcessorType type) {
        APIConfig cfg = endpointConfigs.get(type.name());
        if (cfg == null) {
            throw new IllegalStateException("Missing configuration for processor type: " + type.name() + " under endpoint-configs");
        }
        return deepCopy(cfg);
    }

    private APIConfig deepCopy(APIConfig src) {
        APIConfig copy = new APIConfig();
        copy.setEnable(src.isEnable());
        copy.setResetOnStartup(src.isResetOnStartup());
        copy.setInitPage(src.getInitPage());
        copy.setPageSize(src.getPageSize());
        if (src.getFetchDurationMs() != null) {
            APIConfig.FetchDurationMs fm = new APIConfig.FetchDurationMs();
            fm.setActive(src.getFetchDurationMs().getActive());
            fm.setPassive(src.getFetchDurationMs().getPassive());
            copy.setFetchDurationMs(fm);
        }
        copy.setQueryUrl(src.getQueryUrl());
        // Deep copy notifications map using EnumMap
        if (src.getNotifications() != null) {
            Map<NotificationType, APIConfig.NotificationConfig> notifCopy = new EnumMap<>(NotificationType.class);
            src.getNotifications().forEach((type, v) -> {
                if (v != null && type != null) {
                    APIConfig.NotificationConfig nc = new APIConfig.NotificationConfig();
                    nc.setEnable(v.getEnable());
                    nc.setTopic(v.getTopic());
                    nc.setUrl(v.getUrl());
                    notifCopy.put(type, nc);
                }
            });
            copy.setNotifications(notifCopy);
        }
        return copy;
    }
}
