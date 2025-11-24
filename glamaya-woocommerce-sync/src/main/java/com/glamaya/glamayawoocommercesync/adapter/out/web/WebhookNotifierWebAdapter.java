package com.glamaya.glamayawoocommercesync.adapter.out.web;

import com.glamaya.glamayawoocommercesync.config.webclient.WebhookTarget;
import com.glamaya.glamayawoocommercesync.port.out.WebhookNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WebhookNotifierWebAdapter implements WebhookNotifier {

    private final List<WebClient> clients;

    public WebhookNotifierWebAdapter(@WebhookTarget List<WebClient> clients) {
        this.clients = clients;
    }

    @Override
    public void notifySuccess(String url, Object payload, Map<String, Object> ctx) {
        if (url == null || url.isBlank() || clients == null || clients.isEmpty()) return;
        clients.forEach(client -> client.post().uri(url).bodyValue(payload).retrieve().toEntity(Void.class)
                .doOnSuccess(resp -> log.info("webhook success clientHash={} url={} status={} ctx={}", System.identityHashCode(client), url, resp.getStatusCode().value(), ctx))
                .doOnError(e -> log.error("webhook failure clientHash={} url={} error={} ctx={}", System.identityHashCode(client), url, e.toString(), ctx))
                .onErrorResume(e -> Mono.empty())
                .subscribe());
    }

    @Override
    public void notifyError(String url, Object payload, Map<String, Object> ctx) {
        notifySuccess(url, payload, ctx); // reuse same multi-client logic
    }
}
