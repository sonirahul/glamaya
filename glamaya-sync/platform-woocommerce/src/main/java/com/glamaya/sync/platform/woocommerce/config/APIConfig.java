package com.glamaya.sync.platform.woocommerce.config;

import com.glamaya.sync.core.domain.port.out.ProcessorConfiguration;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API configuration container for WooCommerce module.
 * Values are bound from application-woocommerce.yml (orders-config section).
 */
@Data
@NoArgsConstructor
public class APIConfig implements ProcessorConfiguration<APIConfig> {

    private boolean enable;
    private boolean resetOnStartup;
    private Integer initPage;
    private Integer pageSize;
    private FetchDurationMs fetchDurationMs = new FetchDurationMs();
    private String queryUrl;

    /**
     * Nested object representing fetch duration configuration in milliseconds.
     */
    @Data
    @NoArgsConstructor
    public static class FetchDurationMs {
        private long active;
        private long passive;
    }

    @Override
    public APIConfig get() {
        return this;
    }
}
