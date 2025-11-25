package com.glamaya.glamayawoocommercesync.port.out;

import java.util.Map;

/**
 * Defines the outbound port for sending generic webhook notifications.
 * This interface abstracts the details of how webhooks are sent,
 * allowing the application core to remain decoupled from the specific
 * HTTP client or webhook service implementation.
 */
public interface WebhookNotifier {
    /**
     * Sends a success notification to a specified webhook URL.
     *
     * @param url     The URL of the webhook.
     * @param payload The payload (data) to send with the notification.
     * @param ctx     A context map with additional information relevant to the notification.
     */
    void notifySuccess(String url, Object payload, Map<String, Object> ctx);

    /**
     * Sends an error notification to a specified webhook URL.
     *
     * @param url     The URL of the webhook.
     * @param payload The payload (data) to send with the notification.
     * @param ctx     A context map with additional information relevant to the error.
     */
    void notifyError(String url, Object payload, Map<String, Object> ctx);
}
