package com.glamaya.sync.platform.woocommerce.config;

/**
 * Generic API configuration container for WooCommerce module.
 */
public class APIConfig {

    private boolean enable;
    private boolean resetOnStartup;
    private int pageSize;
    private FetchDurationMs fetchDurationMs = new FetchDurationMs();
    private String queryUrl;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isResetOnStartup() {
        return resetOnStartup;
    }

    public void setResetOnStartup(boolean resetOnStartup) {
        this.resetOnStartup = resetOnStartup;
    }

    public String getPageSize() {
        return String.valueOf(pageSize);
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public FetchDurationMs getFetchDurationMs() {
        return fetchDurationMs;
    }

    public void setFetchDurationMs(FetchDurationMs fetchDurationMs) {
        this.fetchDurationMs = fetchDurationMs;
    }

    public String getQueryUrl() {
        return queryUrl;
    }

    public void setQueryUrl(String queryUrl) {
        this.queryUrl = queryUrl;
    }

    /**
     * Nested object representing fetch duration configuration in milliseconds.
     */
    public static class FetchDurationMs {
        private long active;
        private long passive;

        public long getActive() {
            return active;
        }

        public void setActive(long active) {
            this.active = active;
        }

        public long getPassive() {
            return passive;
        }

        public void setPassive(long passive) {
            this.passive = passive;
        }
    }
}

