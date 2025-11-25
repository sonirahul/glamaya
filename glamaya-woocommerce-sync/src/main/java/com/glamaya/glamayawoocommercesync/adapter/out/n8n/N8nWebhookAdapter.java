package com.glamaya.glamayawoocommercesync.adapter.out.n8n;

import com.glamaya.glamayawoocommercesync.port.out.N8nNotificationPort;
import com.glamaya.glamayawoocommercesync.port.out.WebhookNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * An outbound adapter that implements the {@link N8nNotificationPort}.
 * This adapter uses a generic {@link WebhookNotifier} to send notifications
 * to n8n webhooks, abstracting the HTTP communication details.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class N8nWebhookAdapter implements N8nNotificationPort {
    private final WebhookNotifier webhookNotifier;

    /**
     * Sends a success notification to a specified n8n webhook URL.
     * Delegates the actual sending to the generic {@link WebhookNotifier}.
     *
     * @param successUrl The URL of the n8n webhook for success notifications.
     * @param payload    The payload (data) to send with the notification.
     * @param ctx        A context map with additional information relevant to the notification.
     */
    @Override
    public void sendSuccessNotification(String successUrl, Object payload, Map<String, Object> ctx) {
        if (successUrl == null || successUrl.isBlank()) {
            log.warn("Attempted to send n8n success notification but URL was null or blank.");
            return;
        }
        webhookNotifier.notifySuccess(successUrl, payload, ctx);
    }

    /**
     * Sends an error notification to a specified n8n webhook URL.
     * Delegates the actual sending to the generic {@link WebhookNotifier}.
     *
     * @param errorUrl     The URL of the n8n webhook for error notifications.
     * @param errorMessage The error message to send with the notification.
     * @param ctx          A context map with additional information relevant to the error.
     */
    @Override
    public void sendErrorNotification(String errorUrl, String errorMessage, Map<String, Object> ctx) {
        if (errorUrl == null || errorUrl.isBlank()) {
            log.warn("Attempted to send n8n error notification but URL was null or blank.");
            return;
        }
        webhookNotifier.notifyError(errorUrl, errorMessage, ctx);
    }
}
