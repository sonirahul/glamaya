package com.glamaya.sync.platform.woocommerce.config;

import com.glamaya.sync.core.domain.model.NotificationType;
import com.glamaya.sync.core.domain.port.out.ProcessorConfiguration;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.EnumMap;
import java.util.Map;

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
    private Map<NotificationType, NotificationConfig> notifications = new EnumMap<>(NotificationType.class);

    @Override
    public APIConfig get() {
        return this;
    }

    @Override
    public Long getFetchActiveDelayMs() {
        return fetchDurationMs != null ? fetchDurationMs.getActive() : null;
    }

    @Override
    public ProcessorConfiguration.NotificationConfig getNotificationConfig(NotificationType notificationType) {
        if (notificationType == null) return null;
        NotificationConfig config = notifications.get(notificationType);
        if (config == null) return null;
        return new NotificationConfigCoreImpl(config);
    }

    @Data
    @NoArgsConstructor
    public static class FetchDurationMs {
        private long active;
        private long passive;
    }

    @Data
    @NoArgsConstructor
    public static class NotificationConfig {
        private Boolean enable;
        private String topic;
        private String url;
        // Add other fields as needed for future notification types
    }

    public static class NotificationConfigCoreImpl implements ProcessorConfiguration.NotificationConfig {
        private final NotificationConfig delegate;

        public NotificationConfigCoreImpl(NotificationConfig delegate) {
            this.delegate = delegate;
        }

        @Override
        public Boolean getEnable() {
            return delegate.getEnable();
        }

        @Override
        public String getTopic() {
            return delegate.getTopic();
        }

        @Override
        public String getUrl() {
            return delegate.getUrl();
        }
    }
}
