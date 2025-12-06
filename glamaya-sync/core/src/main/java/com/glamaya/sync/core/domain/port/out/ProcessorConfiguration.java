package com.glamaya.sync.core.domain.port.out;

import com.glamaya.sync.core.domain.model.NotificationType;

/**
 * Strongly-typed configuration contract returned by SyncProcessor implementations.
 * Allows modules to provide their own configuration objects while core treats it generically.
 */
public interface ProcessorConfiguration<T> {
    /**
     * Returns the underlying typed configuration object.
     */
    T get();

    boolean isEnable();

    boolean isResetOnStartup();

    Integer getInitPage();

    Integer getPageSize();

    String getQueryUrl();

    /**
     * Optional pacing configuration in milliseconds.
     * If getFetchActiveDelayMs() > 0, orchestrator will use this fixed delay between page fetches.
     * Otherwise, orchestrator will use exponential backoff capped by getFetchPassiveDelayMs().
     * If passive is null/<=0, orchestrator will apply a sensible default cap.
     */
    Long getFetchActiveDelayMs();
    Long getFetchPassiveDelayMs();

    /**
     * Returns the notification configuration for the given notification type for this processor.
     */
    NotificationConfig getNotificationConfig(NotificationType notificationType);

    /**
     * Core notification configuration abstraction, not platform-specific.
     */
    interface NotificationConfig {
        Boolean getEnable();

        String getTopic();

        String getUrl();
        // Add other generic notification fields as needed
    }
}
