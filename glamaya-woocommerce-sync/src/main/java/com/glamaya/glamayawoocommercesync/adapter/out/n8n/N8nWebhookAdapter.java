package com.glamaya.glamayawoocommercesync.adapter.out.n8n;

import com.glamaya.glamayawoocommercesync.port.out.WebhookNotifier; // Updated import
import com.glamaya.glamayawoocommercesync.port.out.N8nNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class N8nWebhookAdapter implements N8nNotificationPort {
    private final WebhookNotifier webhookNotifier;

    @Override
    public void sendSuccessNotification(String successUrl, Object payload, Map<String, Object> ctx) {
        if (successUrl == null || successUrl.isBlank()) return;
        webhookNotifier.notifySuccess(successUrl, payload, ctx);
    }

    @Override
    public void sendErrorNotification(String errorUrl, String errorMessage, Map<String, Object> ctx) {
        if (errorUrl == null || errorUrl.isBlank()) return;
        webhookNotifier.notifyError(errorUrl, errorMessage, ctx);
    }
}
