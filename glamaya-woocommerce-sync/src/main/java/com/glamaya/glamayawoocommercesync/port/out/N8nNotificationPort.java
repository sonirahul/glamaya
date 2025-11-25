package com.glamaya.glamayawoocommercesync.port.out;

import java.util.Map;

/**
 * Defines the outbound port for sending notifications to n8n webhooks.
 * This interface abstracts the details of how notifications are sent,
 * allowing the application core to remain decoupled from the specific
 * HTTP client or n8n integration details.
 */
public interface N8nNotificationPort {
    /**
     * Sends a success notification to a specified n8n webhook URL.
     *
     * @param successUrl The URL of the n8n webhook for success notifications.
     * @param payload    The payload (data) to send with the notification.
     * @param ctx        A context map with additional information relevant to the notification.
     */
    void sendSuccessNotification(String successUrl, Object payload, Map<String, Object> ctx);

    /**
     * Sends an error notification to a specified n8n webhook URL.
     *
     * @param errorUrl     The URL of the n8n webhook for error notifications.
     * @param errorMessage The error message to send with the notification.
     * @param ctx          A context map with additional information relevant to the error.
     */
    void sendErrorNotification(String errorUrl, String errorMessage, Map<String, Object> ctx);
}
