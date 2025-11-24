package com.glamaya.glamayawoocommercesync.application.service;

import com.glamaya.glamayawoocommercesync.port.out.N8nNotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class N8nApplicationService {
    private final N8nNotificationPort n8nNotificationPort;

    public void success(boolean enable, String successUrl, Object payload, Map<String, Object> ctx) {
        if (!enable) return;
        n8nNotificationPort.sendSuccessNotification(successUrl, payload, ctx);
    }

    public void error(boolean enable, String errorUrl, String errorMessage, Map<String, Object> ctx) {
        if (!enable) return;
        n8nNotificationPort.sendErrorNotification(errorUrl, errorMessage, ctx);
    }
}
