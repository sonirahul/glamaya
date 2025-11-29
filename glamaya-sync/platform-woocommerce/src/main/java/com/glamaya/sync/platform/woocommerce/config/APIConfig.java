package com.glamaya.sync.platform.woocommerce.config;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API configuration container for WooCommerce module.
 */
@Data
@NoArgsConstructor
public class APIConfig {

    private boolean enable;
    private boolean resetOnStartup;
    private int pageSize;
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
}
