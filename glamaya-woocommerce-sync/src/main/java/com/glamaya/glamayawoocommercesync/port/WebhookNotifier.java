package com.glamaya.glamayawoocommercesync.port;

import java.util.Map;

public interface WebhookNotifier {
    void notifySuccess(String url, Object payload, Map<String, Object> ctx);

    void notifyError(String url, Object payload, Map<String, Object> ctx);
}

