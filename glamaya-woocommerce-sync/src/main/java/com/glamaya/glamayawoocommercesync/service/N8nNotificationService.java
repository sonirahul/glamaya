package com.glamaya.glamayawoocommercesync.service;

import com.glamaya.glamayawoocommercesync.port.WebhookNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class N8nNotificationService {
    private final WebhookNotifier webhookNotifier;

    public void success(boolean enable, String successUrl, Object payload, Map<String, Object> ctx) {
        if (!enable || successUrl == null || successUrl.isBlank()) return;
        webhookNotifier.notifySuccess(successUrl, payload, ctx);
    }

    public void error(boolean enable, String errorUrl, String errorMessage, Map<String, Object> ctx) {
        if (!enable || errorUrl == null || errorUrl.isBlank()) return;
        webhookNotifier.notifyError(errorUrl, errorMessage, ctx);
    }
}

