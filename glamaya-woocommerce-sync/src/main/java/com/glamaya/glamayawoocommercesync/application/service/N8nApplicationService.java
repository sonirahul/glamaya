package com.glamaya.glamayawoocommercesync.application.service;

import com.glamaya.glamayawoocommercesync.port.out.N8nNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Application service for sending notifications to n8n.
 * This service acts as a facade over the {@link N8nNotificationPort},
 * providing a clean API for application logic to send success or error notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class N8nApplicationService {
    private final N8nNotificationPort n8nNotificationPort;

    /**
     * Sends a success notification to n8n if the notification is enabled.
     *
     * @param enable     {@code true} to send the notification, {@code false} otherwise.
     * @param successUrl The webhook URL for success notifications.
     * @param payload    The payload to send with the notification.
     * @param ctx        A context map with additional information.
     */
    public void success(boolean enable, String successUrl, Object payload, Map<String, Object> ctx) {
        if (!enable) {
            log.debug("N8n success notification is disabled.");
            return;
        }
        if (successUrl == null || successUrl.isBlank()) {
            log.warn("N8n success webhook URL is not configured. Skipping notification.");
            return;
        }
        n8nNotificationPort.sendSuccessNotification(successUrl, payload, ctx);
        log.info("N8n success notification sent for processor={}", ctx.get("entity"));
    }

    /**
     * Sends an error notification to n8n if the notification is enabled.
     *
     * @param enable       {@code true} to send the notification, {@code false} otherwise.
     * @param errorUrl     The webhook URL for error notifications.
     * @param errorMessage The error message to send.
     * @param ctx          A context map with additional information.
     */
    public void error(boolean enable, String errorUrl, String errorMessage, Map<String, Object> ctx) {
        if (!enable) {
            log.debug("N8n error notification is disabled.");
            return;
        }
        if (errorUrl == null || errorUrl.isBlank()) {
            log.warn("N8n error webhook URL is not configured. Skipping notification.");
            return;
        }
        n8nNotificationPort.sendErrorNotification(errorUrl, errorMessage, ctx);
        log.error("N8n error notification sent for processor={}: {}", ctx.get("entity"), errorMessage);
    }
}
