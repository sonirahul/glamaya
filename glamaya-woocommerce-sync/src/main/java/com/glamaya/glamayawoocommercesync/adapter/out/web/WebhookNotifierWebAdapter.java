package com.glamaya.glamayawoocommercesync.adapter.out.web;

import com.glamaya.glamayawoocommercesync.config.webclient.WebhookTarget;
import com.glamaya.glamayawoocommercesync.port.out.WebhookNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * An outbound adapter that implements the {@link WebhookNotifier} port using Spring's {@link WebClient}.
 * This class is responsible for sending webhook notifications to external systems,
 * abstracting the HTTP communication details.
 */
@Slf4j
@Component
public class WebhookNotifierWebAdapter implements WebhookNotifier {

    private final List<WebClient> clients;

    /**
     * Constructs a new {@code WebhookNotifierWebAdapter}.
     *
     * @param clients A list of {@link WebClient} instances, typically qualified with {@link WebhookTarget}.
     */
    public WebhookNotifierWebAdapter(@WebhookTarget List<WebClient> clients) {
        this.clients = clients;
    }

    /**
     * Sends a success notification to a specified webhook URL.
     * It iterates through configured {@link WebClient}s to send the notification.
     *
     * @param url     The URL of the webhook.
     * @param payload The payload (data) to send with the notification.
     * @param ctx     A context map with additional information relevant to the notification.
     */
    @Override
    public void notifySuccess(String url, Object payload, Map<String, Object> ctx) {
        if (url == null || url.isBlank() || clients == null || clients.isEmpty()) {
            log.warn("Webhook success notification skipped: URL is blank or no WebClient clients configured.");
            return;
        }
        clients.forEach(client -> client.post().uri(url).bodyValue(payload).retrieve().toEntity(Void.class)
                .doOnSuccess(resp -> log.info("Webhook Success: clientHash={} url={} status={} ctx={}", System.identityHashCode(client), url, resp.getStatusCode().value(), ctx))
                .doOnError(e -> log.error("Webhook Failure: clientHash={} url={} error={} ctx={}", System.identityHashCode(client), url, e.toString(), ctx))
                .onErrorResume(e -> Mono.empty()) // Continue processing other clients even if one fails
                .subscribe()); // Non-blocking subscription
    }

    /**
     * Sends an error notification to a specified webhook URL.
     * Currently reuses the same multi-client logic as {@link #notifySuccess(String, Object, Map)}.
     *
     * @param url     The URL of the webhook.
     * @param payload The payload (data) to send with the notification.
     * @param ctx     A context map with additional information relevant to the error.
     */
    @Override
    public void notifyError(String url, Object payload, Map<String, Object> ctx) {
        notifySuccess(url, payload, ctx); // Reuse same multi-client logic for errors
    }
}
