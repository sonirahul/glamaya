package com.glamaya.glamayawoocommercesync.port.out;

import java.util.Map;

public interface N8nNotificationPort {
    void sendSuccessNotification(String successUrl, Object payload, Map<String, Object> ctx);
    void sendErrorNotification(String errorUrl, String errorMessage, Map<String, Object> ctx);
}
